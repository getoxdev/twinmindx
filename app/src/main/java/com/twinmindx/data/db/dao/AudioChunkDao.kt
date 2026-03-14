package com.twinmindx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.twinmindx.data.db.entity.AudioChunkEntity
import com.twinmindx.data.db.entity.ChunkStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunkEntity)

    @Update
    suspend fun update(chunk: AudioChunkEntity)

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksForMeeting(meetingId: String): List<AudioChunkEntity>

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun observeChunksForMeeting(meetingId: String): Flow<List<AudioChunkEntity>>

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId AND status = :status ORDER BY chunkIndex ASC")
    suspend fun getChunksByStatus(meetingId: String, status: ChunkStatus): List<AudioChunkEntity>

    @Query("UPDATE audio_chunks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ChunkStatus)

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId AND status != 'DONE'")
    suspend fun getPendingChunkCount(meetingId: String): Int

    @Query("SELECT * FROM audio_chunks WHERE id = :id")
    suspend fun getChunkById(id: String): AudioChunkEntity?
}
