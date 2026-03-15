package com.twinmindx.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.local.ChunkStatus
import com.twinmindx.data.repository.RecordingRepositoryImpl
import com.twinmindx.domain.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepositoryImpl,
    private val transcriptionRepository: TranscriptionRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "termination_recovery_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            val recoveredMeetingIds = recoverActiveSessions()
            restartTranscribingForRecoveredSessions(recoveredMeetingIds)
            Result.success()
        } catch (e: Exception) {
            Log.e("TerminationWorker", "Termination recovery failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun recoverActiveSessions(): List<String> {
        val activeMeetingIds = recordingRepository.getActiveMeetings()

        activeMeetingIds.forEach { meetingId ->
            val chunks = recordingRepository.getChunksForMeeting(meetingId)
            recordingRepository.finalizeMeeting(meetingId, chunks.size)

            chunks.filter { it.status == ChunkStatus.FAILED }.forEach { chunk ->
                recordingRepository.updateChunkStatus(chunk.id, ChunkStatus.PENDING)
            }
        }

        return activeMeetingIds
    }

    private suspend fun restartTranscribingForRecoveredSessions(meetingIds: List<String>) {
        meetingIds.forEach { meetingId ->
            transcriptionRepository.retryAllChunks(meetingId)
        }
    }
}

