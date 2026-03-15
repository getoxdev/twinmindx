package com.twinmindx.domain.repository

import com.twinmindx.domain.models.TranscriptChunk
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
    fun observeTranscriptForMeeting(meetingId: String): Flow<List<TranscriptChunk>>
    suspend fun getTranscriptForMeeting(meetingId: String): List<TranscriptChunk>
    suspend fun saveTranscriptChunk(
        meetingId: String,
        audioChunkId: String,
        chunkIndex: Int,
        text: String
    )
    suspend fun retryAllChunks(meetingId: String)
}
