package com.twinmindx.data.repository

import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.dao.MeetingDao
import com.twinmindx.data.db.entity.AudioChunkEntity
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingEntity
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.domain.model.Meeting
import com.twinmindx.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptionRepository: TranscriptionRepository
) {

    fun getAllMeetings(): Flow<List<Meeting>> =
        meetingDao.getAllMeetings().map { list -> list.map { it.toDomain() } }

    fun observeMeeting(meetingId: String): Flow<Meeting?> =
        meetingDao.observeMeetingById(meetingId).map { it?.toDomain() }

    suspend fun createMeeting(): MeetingEntity {
        val id = UUID.randomUUID().toString()
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val title = "Meeting - ${formatter.format(Date())}"
        val entity = MeetingEntity(
            id = id,
            title = title,
            startTimeMs = System.currentTimeMillis(),
            endTimeMs = null,
            status = MeetingStatus.RECORDING
        )
        meetingDao.insert(entity)
        return entity
    }

    suspend fun updateMeetingStatus(meetingId: String, status: MeetingStatus) {
        meetingDao.updateStatus(meetingId, status)
    }

    suspend fun finalizeMeeting(meetingId: String, totalChunks: Int) {
        meetingDao.updateEndTime(
            id = meetingId,
            endTimeMs = System.currentTimeMillis(),
            status = MeetingStatus.TRANSCRIBING,
            totalChunks = totalChunks
        )

        val pendingCount = audioChunkDao.getPendingChunkCount(meetingId)
        if (pendingCount == 0) {
            meetingDao.updateStatus(meetingId, MeetingStatus.COMPLETED)
        }
    }

    suspend fun saveAudioChunk(
        meetingId: String,
        chunkIndex: Int,
        filePath: String,
        durationMs: Long
    ): AudioChunkEntity {
        val entity = AudioChunkEntity(
            id = UUID.randomUUID().toString(),
            meetingId = meetingId,
            chunkIndex = chunkIndex,
            filePath = filePath,
            durationMs = durationMs
        )
        audioChunkDao.insert(entity)

        transcriptionRepository.enqueueChunkTranscription(entity)

        return entity
    }

    suspend fun getChunksForMeeting(meetingId: String): List<AudioChunkEntity> =
        audioChunkDao.getChunksForMeeting(meetingId)

    suspend fun updateChunkStatus(chunkId: String, status: ChunkStatus) =
        audioChunkDao.updateStatus(chunkId, status)

    suspend fun getActiveMeetings(): List<MeetingEntity> =
        meetingDao.getMeetingsByStatus(MeetingStatus.RECORDING) +
        meetingDao.getMeetingsByStatus(MeetingStatus.PAUSED)

    suspend fun getMeetingById(meetingId: String): MeetingEntity? =
        meetingDao.getMeetingById(meetingId)
}
