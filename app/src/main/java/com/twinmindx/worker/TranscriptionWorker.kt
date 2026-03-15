package com.twinmindx.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.domain.repositories.RecordingRepository
import com.twinmindx.domain.repositories.TranscriptionRepository
import com.twinmindx.data.transcription.TranscriptionService
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
                return Result.success() // chunk was deleted; nothing to do
            }

        // If already DONE, no work needed
        if (audioChunk.status == ChunkStatus.DONE) {
            return Result.success()
        }

        return try {
            // Mark as in-progress
            audioChunkDao.updateStatus(chunkId, ChunkStatus.TRANSCRIBING)

            val text = transcriptionService.transcribe(audioChunk.filePath, audioChunk.chunkIndex)

            // Persist transcript row
            transcriptionRepository.saveTranscriptChunk(
                meetingId = audioChunk.meetingId,
                audioChunkId = chunkId,
                chunkIndex = audioChunk.chunkIndex,
                text = text
            )

            // Mark chunk done
            audioChunkDao.updateStatus(chunkId, ChunkStatus.DONE)

            // Check if all chunks for this meeting are done → mark meeting COMPLETED
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
                // Restore to PENDING so next attempt picks it up
                audioChunkDao.updateStatus(chunkId, ChunkStatus.PENDING)
                Result.retry()
            } else {
                // Permanent failure — mark chunk and meeting as ERROR
                audioChunkDao.updateStatus(chunkId, ChunkStatus.FAILED)
                recordingRepository.updateMeetingStatus(audioChunk.meetingId, "ERROR")
                Result.failure()
            }
        }
    }
}
