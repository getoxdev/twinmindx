package com.twinmindx.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.twinmindx.data.local.dao.AudioChunkDao
import com.twinmindx.data.local.dao.TranscriptChunkDao
import com.twinmindx.data.local.entity.AudioChunkEntity
import com.twinmindx.data.local.ChunkStatus
import com.twinmindx.data.local.entity.TranscriptChunkEntity
import com.twinmindx.domain.models.TranscriptChunk
import com.twinmindx.domain.models.toDomain
import com.twinmindx.domain.repository.TranscriptionRepository
import com.twinmindx.worker.TranscriptionWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    private val audioChunkDao: AudioChunkDao,
    private val transcriptChunkDao: TranscriptChunkDao,
    private val workManager: WorkManager
) : TranscriptionRepository {

    override fun observeTranscriptForMeeting(meetingId: String): Flow<List<TranscriptChunk>> =
        transcriptChunkDao.observeTranscriptForMeeting(meetingId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTranscriptForMeeting(meetingId: String): List<TranscriptChunk> =
        transcriptChunkDao.getTranscriptForMeeting(meetingId).map { it.toDomain() }

    internal fun enqueueChunkTranscription(chunk: AudioChunkEntity) {
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

    override suspend fun retryAllChunks(meetingId: String) {
        val chunks = audioChunkDao.getChunksForMeeting(meetingId)

        chunks.filter { it.status != ChunkStatus.DONE }.forEach { chunk ->
            transcriptChunkDao.deleteForAudioChunk(chunk.id)
            audioChunkDao.updateStatus(chunk.id, ChunkStatus.PENDING)
        }

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

    override suspend fun saveTranscriptChunk(
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
