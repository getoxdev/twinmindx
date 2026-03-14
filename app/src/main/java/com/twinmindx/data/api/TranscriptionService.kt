package com.twinmindx.data.api

import java.io.File

interface TranscriptionService {
    suspend fun transcribe(audioFile: File, chunkIndex: Int): String
}
