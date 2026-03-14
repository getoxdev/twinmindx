package com.twinmindx.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.api.SummaryService
import com.twinmindx.data.db.entity.SummaryStatus
import com.twinmindx.data.repository.SummaryRepository
import com.twinmindx.data.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryService: SummaryService,
    private val summaryRepository: SummaryRepository,
    private val transcriptionRepository: TranscriptionRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MEETING_ID = "meeting_id"
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getString(KEY_MEETING_ID) ?: return Result.failure()

        val transcript = transcriptionRepository.getFullTranscript(meetingId)
        if (transcript.isBlank()) return Result.failure()

        summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.STREAMING)

        var accumulatedTitle = ""
        var accumulatedSummary = ""
        var accumulatedActions = ""
        var accumulatedPoints = ""

        return try {
            summaryService.summarize(transcript)
                .onEach { chunk ->
                    chunk.title?.let { accumulatedTitle = it }
                    chunk.summary?.let { accumulatedSummary = it }
                    chunk.actionItems?.let { accumulatedActions = it }
                    chunk.keyPoints?.let { accumulatedPoints = it }

                    summaryRepository.updateSummaryContent(
                        meetingId = meetingId,
                        title = accumulatedTitle,
                        summary = accumulatedSummary,
                        actionItems = accumulatedActions,
                        keyPoints = accumulatedPoints,
                        status = SummaryStatus.STREAMING
                    )
                }
                .catch { e ->
                    summaryRepository.updateSummaryStatus(
                        meetingId = meetingId,
                        status = SummaryStatus.ERROR,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                .collect()

            summaryRepository.updateSummaryContent(
                meetingId = meetingId,
                title = accumulatedTitle,
                summary = accumulatedSummary,
                actionItems = accumulatedActions,
                keyPoints = accumulatedPoints,
                status = SummaryStatus.COMPLETED
            )
            Result.success()
        } catch (e: Exception) {
            summaryRepository.updateSummaryStatus(
                meetingId = meetingId,
                status = SummaryStatus.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            Result.failure()
        }
    }
}
