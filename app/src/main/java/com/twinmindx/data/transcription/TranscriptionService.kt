package com.twinmindx.data.transcription

/**
 * Interface for transcription backends.
 * Implementations may use real APIs (Whisper, Gemini) or mock responses.
 */
interface TranscriptionService {
    /**
     * Transcribes an audio file at the given path.
     * @param filePath Absolute path to the audio file.
     * @param chunkIndex The sequential index of this chunk within the meeting (used for mock text).
     * @return The transcribed text.
     * @throws TranscriptionException if transcription fails.
     */
    suspend fun transcribe(filePath: String, chunkIndex: Int): String
}

class TranscriptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
