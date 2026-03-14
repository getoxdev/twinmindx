package com.twinmindx.data.summary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) {

    companion object {
        private const val TAG = "OpenAiSummaryService"
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
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

        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            })
        }.toString()

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            response.close()
            val errorMessage = parseApiError(errorBody, response.code)
            throw IOException(errorMessage)
        }

        val responseBody = response.body ?: run {
            response.close()
            throw IOException("Empty response from OpenAI")
        }

        val accumulated = StringBuilder()

        responseBody.source().use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val json = JSONObject(data)
                        val delta = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                            .optString("content", "")

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
            val json = JSONObject(errorBody)
            val message = json.optJSONObject("error")?.optString("message") ?: errorBody
            "OpenAI API error ($code): $message"
        } catch (_: Exception) {
            "OpenAI API error ($code): $errorBody"
        }
    }
}
