package com.twinmindx.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twinmindx.data.local.entity.TranscriptChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: TranscriptChunkEntity)

    @Query("SELECT * FROM transcript_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun observeTranscriptForMeeting(meetingId: String): Flow<List<TranscriptChunkEntity>>

    @Query("SELECT * FROM transcript_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getTranscriptForMeeting(meetingId: String): List<TranscriptChunkEntity>

    @Query("DELETE FROM transcript_chunks WHERE audioChunkId = :audioChunkId")
    suspend fun deleteForAudioChunk(audioChunkId: String)

    @Query("DELETE FROM transcript_chunks WHERE meetingId = :meetingId")
    suspend fun deleteForMeeting(meetingId: String)
}
