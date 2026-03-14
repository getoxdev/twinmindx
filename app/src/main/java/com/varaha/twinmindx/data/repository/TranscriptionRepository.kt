package com.varaha.twinmindx.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.varaha.twinmindx.data.db.dao.TranscriptChunkDao
import com.varaha.twinmindx.data.db.entity.TranscriptChunkEntity
import com.varaha.twinmindx.worker.TranscriptionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptChunkDao: TranscriptChunkDao,
    private val workManager: WorkManager
) {

    fun enqueueTranscription(meetingId: String, chunkId: String, chunkIndex: Int, filePath: String) {
        val inputData = Data.Builder()
            .putString(TranscriptionWorker.KEY_MEETING_ID, meetingId)
            .putString(TranscriptionWorker.KEY_CHUNK_ID, chunkId)
            .putInt(TranscriptionWorker.KEY_CHUNK_INDEX, chunkIndex)
            .putString(TranscriptionWorker.KEY_FILE_PATH, filePath)
            .build()

        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("transcription_$meetingId")
            .build()

        workManager.enqueueUniqueWork(
            "transcription_${meetingId}_$chunkIndex",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun observeTranscriptChunks(meetingId: String): Flow<List<TranscriptChunkEntity>> =
        transcriptChunkDao.observeTranscriptChunks(meetingId)

    suspend fun getFullTranscript(meetingId: String): String =
        transcriptChunkDao.getOrderedTranscriptTexts(meetingId).joinToString(" ")

    suspend fun saveTranscriptChunk(meetingId: String, chunkIndex: Int, text: String) {
        val entity = TranscriptChunkEntity(
            id = "${meetingId}_$chunkIndex",
            meetingId = meetingId,
            chunkIndex = chunkIndex,
            text = text
        )
        transcriptChunkDao.insert(entity)
    }
}
