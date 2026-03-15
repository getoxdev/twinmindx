package com.twinmindx.data.remote.transcription

import android.util.Log
import com.twinmindx.BuildConfig
import com.twinmindx.data.remote.transcription.network.WhisperApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionServiceImpl @Inject constructor(
    private val whisperApiService: WhisperApiService
) : TranscriptionService {

    companion object {
        private const val TAG = "OpenAIWhisperService"
        private const val MODEL_WHISPER = "whisper-1"
        private const val RESPONSE_FORMAT_JSON = "json"
        private val AUDIO_MEDIA_TYPE = "audio/wav".toMediaTypeOrNull()
    }

    override suspend fun transcribe(filePath: String, chunkIndex: Int): String {
        return withContext(Dispatchers.IO) {
            val audioFile = File(filePath)

            if (!audioFile.exists()) {
                throw IOException("Audio file not found: $filePath")
            }

            if (audioFile.length() == 0L) {
                throw IOException("Audio file is empty: $filePath")
            }

            Log.d(TAG, "Transcribing chunk $chunkIndex, file size: ${audioFile.length()} bytes")

            val requestFile = audioFile.asRequestBody(AUDIO_MEDIA_TYPE)
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val modelPart = MODEL_WHISPER.toRequestBody("text/plain".toMediaTypeOrNull())
            val languagePart = "en".toRequestBody("text/plain".toMediaTypeOrNull())
            val responseFormatPart = RESPONSE_FORMAT_JSON.toRequestBody("text/plain".toMediaTypeOrNull())

            try {
                val response = whisperApiService.transcribeAudio(
                    authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                    file = filePart,
                    model = modelPart,
                    language = languagePart,
                    responseFormat = responseFormatPart
                )

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Whisper API error (${response.code()}): $errorBody")
                    throw IOException("Whisper API error (${response.code()}): $errorBody")
                }

                val transcriptionResponse = response.body()
                    ?: throw IOException("Empty response from Whisper API")

                if (transcriptionResponse.error != null) {
                    Log.e(TAG, "Whisper API error: ${transcriptionResponse.error.message}")
                    throw IOException("Whisper API error: ${transcriptionResponse.error.message}")
                }

                val transcribedText = transcriptionResponse.text.trim()

                if (transcribedText.isEmpty()) {
                    Log.w(TAG, "Chunk $chunkIndex returned empty transcription")
                } else {
                    Log.d(TAG, "Chunk $chunkIndex transcribed successfully (${transcribedText.length} chars)")
                }

                transcribedText

            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error transcribing chunk $chunkIndex", e)
                throw IOException("HTTP error ${e.code()}: ${e.message()}", e)
            } catch (e: IOException) {
                Log.e(TAG, "IO error transcribing chunk $chunkIndex", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error transcribing chunk $chunkIndex", e)
                throw IOException("Transcription failed: ${e.message}", e)
            }
        }
    }
}
