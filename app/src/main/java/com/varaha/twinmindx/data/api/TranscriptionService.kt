package com.varaha.twinmindx.data.api

import java.io.File

interface TranscriptionService {
    suspend fun transcribe(audioFile: File, chunkIndex: Int): String
}
