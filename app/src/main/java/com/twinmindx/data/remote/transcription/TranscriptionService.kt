package com.twinmindx.data.remote.transcription

interface TranscriptionService {
    suspend fun transcribe(filePath: String, chunkIndex: Int): String
}
