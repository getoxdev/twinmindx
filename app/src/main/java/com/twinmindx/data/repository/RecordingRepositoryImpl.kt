package com.twinmindx.data.repository

import com.twinmindx.data.local.dao.AudioChunkDao
import com.twinmindx.data.local.dao.MeetingDao
import com.twinmindx.data.local.entity.AudioChunkEntity
import com.twinmindx.data.local.entity.MeetingEntity
import com.twinmindx.data.local.entity.MeetingStatus
import com.twinmindx.domain.models.Meeting
import com.twinmindx.domain.models.toDomain
import com.twinmindx.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptionRepository: TranscriptionRepositoryImpl
) : RecordingRepository {

    override fun getAllMeetings(): Flow<List<Meeting>> =
        meetingDao.getAllMeetings().map { list -> list.map { it.toDomain() } }

    override fun observeMeeting(meetingId: String): Flow<Meeting?> =
        meetingDao.observeMeetingById(meetingId).map { it?.toDomain() }

    override suspend fun createMeeting(): String {
        val id = UUID.randomUUID().toString()
        val title = "Untitled Note"
        val entity = MeetingEntity(
            id = id,
            title = title,
            startTimeMs = System.currentTimeMillis(),
            endTimeMs = null,
            status = MeetingStatus.RECORDING
        )
        meetingDao.insert(entity)
        return id
    }

    override suspend fun updateMeetingStatus(meetingId: String, status: String) {
        val meetingStatus = MeetingStatus.valueOf(status)
        meetingDao.updateStatus(meetingId, meetingStatus)
    }

    override suspend fun updateMeetingTitle(meetingId: String, title: String) {
        meetingDao.updateTitle(meetingId, title)
    }

    override suspend fun finalizeMeeting(meetingId: String, totalChunks: Int) {
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

    override suspend fun saveAudioChunk(
        meetingId: String,
        chunkIndex: Int,
        filePath: String,
        durationMs: Long
    ): String {
        val entity = AudioChunkEntity(
            id = UUID.randomUUID().toString(),
            meetingId = meetingId,
            chunkIndex = chunkIndex,
            filePath = filePath,
            durationMs = durationMs
        )
        audioChunkDao.insert(entity)

        transcriptionRepository.enqueueChunkTranscription(entity)

        return entity.id
    }

    suspend fun getChunksForMeeting(meetingId: String): List<AudioChunkEntity> =
        audioChunkDao.getChunksForMeeting(meetingId)

    suspend fun updateChunkStatus(chunkId: String, status: com.twinmindx.data.local.entity.ChunkStatus) =
        audioChunkDao.updateStatus(chunkId, status)

    override suspend fun getActiveMeetings(): List<String> {
        val recording = meetingDao.getMeetingsByStatus(MeetingStatus.RECORDING)
        val paused = meetingDao.getMeetingsByStatus(MeetingStatus.PAUSED)
        return (recording + paused).map { it.id }
    }

    override suspend fun getMeetingById(meetingId: String): Meeting? =
        meetingDao.getMeetingById(meetingId)?.toDomain()
}
