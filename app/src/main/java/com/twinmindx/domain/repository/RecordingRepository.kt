package com.twinmindx.domain.repository

import com.twinmindx.domain.models.Meeting
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllMeetings(): Flow<List<Meeting>>
    fun observeMeeting(meetingId: String): Flow<Meeting?>
    suspend fun createMeeting(): String
    suspend fun updateMeetingStatus(meetingId: String, status: String)
    suspend fun finalizeMeeting(meetingId: String, totalChunks: Int)
    suspend fun saveAudioChunk(
        meetingId: String,
        chunkIndex: Int,
        filePath: String,
        durationMs: Long
    ): String
    suspend fun getActiveMeetings(): List<String>
    suspend fun getMeetingById(meetingId: String): Meeting?
}
