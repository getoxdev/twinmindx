package com.varaha.twinmindx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.varaha.twinmindx.data.db.entity.TranscriptChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: TranscriptChunkEntity)

    @Query("SELECT * FROM transcript_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getTranscriptChunks(meetingId: String): List<TranscriptChunkEntity>

    @Query("SELECT * FROM transcript_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun observeTranscriptChunks(meetingId: String): Flow<List<TranscriptChunkEntity>>

    @Query("SELECT text FROM transcript_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getOrderedTranscriptTexts(meetingId: String): List<String>

    @Query("DELETE FROM transcript_chunks WHERE meetingId = :meetingId")
    suspend fun deleteAllForMeeting(meetingId: String)
}
