package com.twinmindx.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.dao.TranscriptChunkDao
import com.twinmindx.data.db.entity.AudioChunkEntity
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.TranscriptChunkEntity
import com.twinmindx.worker.TranscriptionWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    private val audioChunkDao: AudioChunkDao,
    private val transcriptChunkDao: TranscriptChunkDao,
    private val workManager: WorkManager
) {

    fun observeTranscriptForMeeting(meetingId: String): Flow<List<TranscriptChunkEntity>> =
        transcriptChunkDao.observeTranscriptForMeeting(meetingId)

    suspend fun getTranscriptForMeeting(meetingId: String): List<TranscriptChunkEntity> =
        transcriptChunkDao.getTranscriptForMeeting(meetingId)

    /**
     * Enqueues a [TranscriptionWorker] for the given audio chunk.
     * Uses a unique work name per chunk so duplicate workers are avoided.
     */
    fun enqueueChunkTranscription(chunk: AudioChunkEntity) {
        val inputData = Data.Builder()
            .putString(TranscriptionWorker.KEY_CHUNK_ID, chunk.id)
            .build()

        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            TranscriptionWorker.workName(chunk.id),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Resets all non-DONE chunks for [meetingId] back to PENDING,
     * removes their stale transcript rows, and re-enqueues workers.
     * This implements the "retry ALL audio on failure" requirement.
     */
    suspend fun retryAllChunks(meetingId: String) {
        val chunks = audioChunkDao.getChunksForMeeting(meetingId)

        chunks.filter { it.status != ChunkStatus.DONE }.forEach { chunk ->
            // Clear any partial transcript for this chunk
            transcriptChunkDao.deleteForAudioChunk(chunk.id)
            // Reset status so the worker will pick it up
            audioChunkDao.updateStatus(chunk.id, ChunkStatus.PENDING)
        }

        // Re-enqueue all non-DONE chunks (replace existing work so it runs fresh)
        chunks.filter { it.status != ChunkStatus.DONE }.forEach { chunk ->
            val inputData = Data.Builder()
                .putString(TranscriptionWorker.KEY_CHUNK_ID, chunk.id)
                .build()

            val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                TranscriptionWorker.workName(chunk.id),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    suspend fun saveTranscriptChunk(
        meetingId: String,
        audioChunkId: String,
        chunkIndex: Int,
        text: String
    ) {
        val entity = TranscriptChunkEntity(
            id = UUID.randomUUID().toString(),
            meetingId = meetingId,
            audioChunkId = audioChunkId,
            chunkIndex = chunkIndex,
            text = text
        )
        transcriptChunkDao.insert(entity)
    }
}
