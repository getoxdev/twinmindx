package com.twinmindx.data.transcription

interface TranscriptionService {
    suspend fun transcribe(filePath: String, chunkIndex: Int): String
}
