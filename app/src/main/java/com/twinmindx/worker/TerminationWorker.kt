package com.twinmindx.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.data.repository.RecordingRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepositoryImpl,
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
        val activeMeetingIds = recordingRepository.getActiveMeetings()

        activeMeetingIds.forEach { meetingId ->
            val chunks = recordingRepository.getChunksForMeeting(meetingId)
            recordingRepository.finalizeMeeting(meetingId, chunks.size)

            chunks.filter {
                it.status == ChunkStatus.PENDING ||
                it.status == ChunkStatus.TRANSCRIBING ||
                it.status == ChunkStatus.FAILED
            }.forEach { chunk ->
                if (chunk.status == ChunkStatus.FAILED) {
                    recordingRepository.updateChunkStatus(chunk.id, ChunkStatus.PENDING)
                }
            }

            val meeting = recordingRepository.getMeetingById(meetingId)
            if (meeting?.status == MeetingStatus.RECORDING || meeting?.status == MeetingStatus.PAUSED) {
                recordingRepository.updateMeetingStatus(meetingId, "TRANSCRIBING")
            }
        }
    }
}
