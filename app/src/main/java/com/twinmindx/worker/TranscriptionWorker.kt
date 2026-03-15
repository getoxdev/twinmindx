package com.twinmindx.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.local.dao.AudioChunkDao
import com.twinmindx.data.local.entity.ChunkStatus
import com.twinmindx.data.local.entity.MeetingStatus
import com.twinmindx.domain.repository.RecordingRepository
import com.twinmindx.domain.repository.TranscriptionRepository
import com.twinmindx.data.remote.transcription.TranscriptionService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptionService: TranscriptionService,
    private val transcriptionRepository: TranscriptionRepository,
    private val recordingRepository: RecordingRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_CHUNK_ID = "chunk_id"
        private const val MAX_ATTEMPTS = 3
        private const val TAG = "TranscriptionWorker"

        fun workName(chunkId: String) = "transcription_$chunkId"
    }

    override suspend fun doWork(): Result {
        val chunkId = inputData.getString(KEY_CHUNK_ID)
            ?: return Result.failure()

        val audioChunk = audioChunkDao.getChunkById(chunkId)
            ?: run {
                Log.w(TAG, "Chunk $chunkId not found — skipping.")
                return Result.success()
            }

        if (audioChunk.status == ChunkStatus.DONE) {
            return Result.success()
        }

        return try {
            audioChunkDao.updateStatus(chunkId, ChunkStatus.TRANSCRIBING)

            val text = transcriptionService.transcribe(audioChunk.filePath, audioChunk.chunkIndex)

            transcriptionRepository.saveTranscriptChunk(
                meetingId = audioChunk.meetingId,
                audioChunkId = chunkId,
                chunkIndex = audioChunk.chunkIndex,
                text = text
            )

            audioChunkDao.updateStatus(chunkId, ChunkStatus.DONE)

            val remainingCount = audioChunkDao.getPendingChunkCount(audioChunk.meetingId)
            if (remainingCount == 0) {
                val meeting = recordingRepository.getMeetingById(audioChunk.meetingId)
                if (meeting?.status == MeetingStatus.TRANSCRIBING) {
                    recordingRepository.updateMeetingStatus(
                        audioChunk.meetingId, "COMPLETED"
                    )
                }
            }

            Log.d(TAG, "Chunk ${audioChunk.chunkIndex} transcribed successfully.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed for chunk $chunkId (attempt $runAttemptCount)", e)

            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                audioChunkDao.updateStatus(chunkId, ChunkStatus.PENDING)
                Result.retry()
            } else {

                audioChunkDao.updateStatus(chunkId, ChunkStatus.FAILED)
                recordingRepository.updateMeetingStatus(audioChunk.meetingId, "ERROR")
                Result.failure()
            }
        }
    }
}
