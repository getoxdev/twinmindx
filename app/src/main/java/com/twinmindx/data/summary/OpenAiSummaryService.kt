package com.twinmindx.data.summary

import android.util.Log
import com.google.gson.Gson
import com.twinmindx.data.summary.network.ChatCompletionRequest
import com.twinmindx.data.summary.network.Choice
import com.twinmindx.data.summary.network.Message
import com.twinmindx.data.summary.network.OpenAiApi
import com.twinmindx.data.summary.network.OpenAiErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SummaryResult(
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>
)

@Singleton
class OpenAiSummaryService @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val apiKey: String,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "OpenAiSummaryService"
        private const val MODEL = "gpt-4o-mini"

        private val SYSTEM_PROMPT = """
            You are a helpful assistant that analyzes meeting transcripts.
            Return ONLY a valid JSON object (no markdown, no code fences) with these exact keys:
            {
              "title": "A concise title for this meeting (max 10 words)",
              "summary": "A clear paragraph summarizing the meeting",
              "actionItems": ["action item 1", "action item 2"],
              "keyPoints": ["key point 1", "key point 2"]
            }
        """.trimIndent()
    }

    fun streamSummary(transcript: String): Flow<String> = flow {
        val userMessage = "Please analyze this meeting transcript and return the JSON summary:\n\n$transcript"

        val request = ChatCompletionRequest(
            model = MODEL,
            stream = true,
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = userMessage)
            )
        )

        val response = openAiApi.createChatCompletionStreaming(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            val errorMessage = parseApiError(errorBody, response.code())
            throw IOException(errorMessage)
        }

        val responseBody = response.body() ?: throw IOException("Empty response from OpenAI")

        val accumulated = StringBuilder()

        responseBody.source().use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val streamResponse = gson.fromJson(data, com.twinmindx.data.summary.network.ChatCompletionResponse::class.java)
                        val delta = streamResponse.choices?.firstOrNull()?.delta?.content ?: ""

                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            emit(accumulated.toString())
                        }
                    } catch (_: Exception) { // Ignore malformed SSE lines; they're sometimes empty
                        Log.v(TAG, "Skipping SSE line: $data")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun parseSummaryResult(rawJson: String): SummaryResult {
        val cleaned = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)
        val title = json.optString("title", "").ifBlank { "Meeting Summary" }
        val summary = json.optString("summary", "")
        val actionItemsArray = json.optJSONArray("actionItems") ?: JSONArray()
        val keyPointsArray = json.optJSONArray("keyPoints") ?: JSONArray()

        val actionItems = (0 until actionItemsArray.length()).map { actionItemsArray.getString(it) }
        val keyPoints = (0 until keyPointsArray.length()).map { keyPointsArray.getString(it) }

        return SummaryResult(
            title = title,
            summary = summary,
            actionItems = actionItems,
            keyPoints = keyPoints
        )
    }

    private fun parseApiError(errorBody: String, code: Int): String {
        return try {
            val errorResponse = gson.fromJson(errorBody, OpenAiErrorResponse::class.java)
            val message = errorResponse.error?.message ?: errorBody
            "OpenAI API error ($code): $message"
        } catch (_: Exception) {
            "OpenAI API error ($code): $errorBody"
        }
    }
}
