package com.twinmindx.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.data.repository.RecordingRepository
import com.twinmindx.data.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that runs on app startup to recover from unexpected process termination.
 * Finalizes any active meetings that were recording when the app was killed,
 * and re-enqueues transcription jobs for pending chunks.
 */
@HiltWorker
class TerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "termination_recovery_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            recoverActiveSessions()
            Result.success()
        } catch (e: Exception) {
            // Retry on failure, as this is critical for data integrity
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun recoverActiveSessions() {
        // Get all meetings that were active when the process died
        val activeMeetings = recordingRepository.getActiveMeetings()

        activeMeetings.forEach { meeting ->
            // Get all chunks for this meeting
            val chunks = recordingRepository.getChunksForMeeting(meeting.id)

            // Finalize the meeting
            recordingRepository.finalizeMeeting(meeting.id, chunks.size)

            // Re-enqueue transcription for any pending or failed chunks
            chunks.filter {
                it.status == ChunkStatus.PENDING ||
                it.status == ChunkStatus.TRANSCRIBING ||
                it.status == ChunkStatus.FAILED
            }.forEach { chunk ->
                // Reset failed chunks to pending
                if (chunk.status == ChunkStatus.FAILED) {
                    recordingRepository.updateChunkStatus(chunk.id, ChunkStatus.PENDING)
                }

                // Enqueue transcription work
                transcriptionRepository.enqueueTranscription(
                    meetingId = meeting.id,
                    chunkId = chunk.id,
                    chunkIndex = chunk.chunkIndex,
                    filePath = chunk.filePath
                )
            }

            // Update meeting status if it was still in RECORDING/PAUSED
            if (meeting.status == MeetingStatus.RECORDING || meeting.status == MeetingStatus.PAUSED) {
                recordingRepository.updateMeetingStatus(meeting.id, MeetingStatus.TRANSCRIBING)
            }
        }
    }
}
