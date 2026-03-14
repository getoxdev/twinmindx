package com.twinmindx.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.api.TranscriptionService
import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.repository.RecordingRepository
import com.twinmindx.data.repository.SummaryRepository
import com.twinmindx.data.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionService: TranscriptionService,
    private val transcriptionRepository: TranscriptionRepository,
    private val summaryRepository: SummaryRepository,
    private val recordingRepository: RecordingRepository,
    private val audioChunkDao: AudioChunkDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MEETING_ID = "meeting_id"
        const val KEY_CHUNK_ID = "chunk_id"
        const val KEY_CHUNK_INDEX = "chunk_index"
        const val KEY_FILE_PATH = "file_path"
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getString(KEY_MEETING_ID) ?: return Result.failure()
        val chunkId = inputData.getString(KEY_CHUNK_ID) ?: return Result.failure()
        val chunkIndex = inputData.getInt(KEY_CHUNK_INDEX, -1)
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()

        if (chunkIndex == -1) return Result.failure()

        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        audioChunkDao.updateStatus(chunkId, ChunkStatus.TRANSCRIBING)

        return try {
            val transcript = transcriptionService.transcribe(file, chunkIndex)
            transcriptionRepository.saveTranscriptChunk(meetingId, chunkIndex, transcript)
            audioChunkDao.updateStatus(chunkId, ChunkStatus.DONE)

            checkAndTriggerSummary(meetingId)
            Result.success()
        } catch (e: Exception) {
            audioChunkDao.updateStatus(chunkId, ChunkStatus.FAILED)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                retryAllFailedChunks(meetingId)
                Result.failure()
            }
        }
    }

    private suspend fun retryAllFailedChunks(meetingId: String) {
        val failedChunks = audioChunkDao.getChunksByStatus(meetingId, ChunkStatus.FAILED)
        failedChunks.forEach { chunk ->
            audioChunkDao.updateStatus(chunk.id, ChunkStatus.PENDING)
            transcriptionRepository.enqueueTranscription(
                meetingId = meetingId,
                chunkId = chunk.id,
                chunkIndex = chunk.chunkIndex,
                filePath = chunk.filePath
            )
        }
    }

    private suspend fun checkAndTriggerSummary(meetingId: String) {
        val meeting = recordingRepository.getMeetingById(meetingId) ?: return
        if (meeting.totalChunks <= 0) return

        val pendingCount = audioChunkDao.getPendingChunkCount(meetingId)
        if (pendingCount == 0) {
            summaryRepository.initializeSummary(meetingId)
            summaryRepository.enqueueSummaryGeneration(meetingId)
        }
    }
}
