package com.twinmindx.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.twinmindx.data.local.entity.MeetingEntity
import com.twinmindx.data.local.entity.MeetingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity)

    @Update
    suspend fun update(meeting: MeetingEntity)

    @Query("SELECT * FROM meetings ORDER BY startTimeMs DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: String): MeetingEntity?

    @Query("SELECT * FROM meetings WHERE id = :id")
    fun observeMeetingById(id: String): Flow<MeetingEntity?>

    @Query("SELECT * FROM meetings WHERE status = :status")
    suspend fun getMeetingsByStatus(status: MeetingStatus): List<MeetingEntity>

    @Query("UPDATE meetings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: MeetingStatus)

    @Query("UPDATE meetings SET endTimeMs = :endTimeMs, status = :status, totalChunks = :totalChunks WHERE id = :id")
    suspend fun updateEndTime(id: String, endTimeMs: Long, status: MeetingStatus, totalChunks: Int)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteById(id: String)
}
