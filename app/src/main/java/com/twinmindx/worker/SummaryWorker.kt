package com.twinmindx.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmindx.data.db.dao.SummaryDao
import com.twinmindx.data.db.entity.SummaryStatus
import com.twinmindx.domain.repositories.TranscriptionRepository
import com.twinmindx.data.summary.OpenAiSummaryService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withTimeout
import org.json.JSONArray

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryDao: SummaryDao,
    private val transcriptionRepository: TranscriptionRepository,
    private val openAiSummaryService: OpenAiSummaryService
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MEETING_ID = "meeting_id"
        private const val MAX_ATTEMPTS = 3
        private const val TAG = "SummaryWorker"
        private const val STREAM_UPDATE_INTERVAL = 20
        private const val SUMMARY_GENERATION_TIMEOUT_MS = 75000L

        fun workName(meetingId: String) = "summary_$meetingId"
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getString(KEY_MEETING_ID)
            ?: return Result.failure()

        Log.d(TAG, "Starting summary generation for meeting: $meetingId (attempt $runAttemptCount)")

        val chunks = transcriptionRepository.getTranscriptForMeeting(meetingId)
        if (chunks.isEmpty()) {
            Log.w(TAG, "No transcript chunks found for meeting $meetingId")
            summaryDao.updateError(
                meetingId = meetingId,
                status = SummaryStatus.ERROR,
                errorMessage = "No transcript available to summarize.",
                updatedAtMs = System.currentTimeMillis()
            )
            return Result.failure()
        }

        val transcript = chunks
            .sortedBy { it.chunkIndex }
            .joinToString("\n\n") { it.text }

        summaryDao.updateStatus(
            meetingId = meetingId,
            status = SummaryStatus.GENERATING,
            updatedAtMs = System.currentTimeMillis()
        )

        return try {
            var tokenBuffer = 0
            var lastEmittedText = ""

            withTimeout(SUMMARY_GENERATION_TIMEOUT_MS) {
                openAiSummaryService.streamSummary(transcript).collect { accumulatedText ->
                    lastEmittedText = accumulatedText
                    tokenBuffer++

                    if (tokenBuffer >= STREAM_UPDATE_INTERVAL) {
                        summaryDao.updateStreamingText(
                            meetingId = meetingId,
                            text = accumulatedText,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    }
                }
            }

            if (lastEmittedText.isNotEmpty()) {
                summaryDao.updateStreamingText(
                    meetingId = meetingId,
                    text = lastEmittedText,
                    updatedAtMs = System.currentTimeMillis()
                )
            }

            val result = openAiSummaryService.parseSummaryResult(lastEmittedText)

            summaryDao.updateCompleted(
                meetingId = meetingId,
                title = result.title,
                summary = result.summary,
                actionItems = listToJson(result.actionItems),
                keyPoints = listToJson(result.keyPoints),
                status = SummaryStatus.COMPLETED,
                updatedAtMs = System.currentTimeMillis()
            )

            Log.d(TAG, "Summary generation completed for meeting: $meetingId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed for meeting $meetingId (attempt $runAttemptCount)", e)

            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                summaryDao.updateStatus(
                    meetingId = meetingId,
                    status = SummaryStatus.PENDING,
                    updatedAtMs = System.currentTimeMillis()
                )
                Result.retry()
            } else {
                val errorMessage = buildErrorMessage(e)
                summaryDao.updateError(
                    meetingId = meetingId,
                    status = SummaryStatus.ERROR,
                    errorMessage = errorMessage,
                    updatedAtMs = System.currentTimeMillis()
                )
                Result.failure()
            }
        }
    }

    private fun listToJson(items: List<String>): String {
        val array = JSONArray()
        items.forEach { array.put(it) }
        return array.toString()
    }

    private fun buildErrorMessage(e: Exception): String {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("unable to resolve host") ||
            message.contains("unknown host") ||
            message.contains("no route to host") ||
            message.contains("network is unreachable") ||
            message.contains("failed to connect") ->
                "No internet connection. Please check your network and retry."

            message.contains("timeout") ||
            message.contains("timed out") ||
            e is kotlinx.coroutines.TimeoutCancellationException ->
                "Request timed out. Please check your connection and retry."

            message.startsWith("openai api error") ->
                e.message ?: "OpenAI API error"

            message.contains("401") ->
                "Invalid API key. Please check your OpenAI API key."

            message.contains("429") ->
                "Rate limit exceeded. Please wait a moment and retry."

            message.contains("socket") ||
            message.contains("ssl") ||
            message.contains("handshake") ->
                "Network error. Please check your connection and retry."

            else -> "Failed to generate summary: ${e.message ?: "Unknown error"}"
        }
    }
}
