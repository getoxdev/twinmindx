package com.twinmindx.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.data.repository.RecordingRepository
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
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "termination_recovery_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            recoverActiveSessions()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun recoverActiveSessions() {
        val activeMeetings = recordingRepository.getActiveMeetings()

        activeMeetings.forEach { meeting ->
            val chunks = recordingRepository.getChunksForMeeting(meeting.id)
            recordingRepository.finalizeMeeting(meeting.id, chunks.size)

            chunks.filter {
                it.status == ChunkStatus.PENDING ||
                it.status == ChunkStatus.TRANSCRIBING ||
                it.status == ChunkStatus.FAILED
            }.forEach { chunk ->
                if (chunk.status == ChunkStatus.FAILED) {
                    recordingRepository.updateChunkStatus(chunk.id, ChunkStatus.PENDING)
                }
            }

            // Update meeting status if it was still in RECORDING/PAUSED
            if (meeting.status == MeetingStatus.RECORDING || meeting.status == MeetingStatus.PAUSED) {
                recordingRepository.updateMeetingStatus(meeting.id, MeetingStatus.TRANSCRIBING)
            }
        }
    }
}
