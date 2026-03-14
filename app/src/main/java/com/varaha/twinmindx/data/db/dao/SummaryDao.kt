package com.varaha.twinmindx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varaha.twinmindx.data.db.entity.SummaryEntity
import com.varaha.twinmindx.data.db.entity.SummaryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity)

    @Update
    suspend fun update(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryForMeeting(meetingId: String): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun observeSummaryForMeeting(meetingId: String): Flow<SummaryEntity?>

    @Query("UPDATE summaries SET title = :title, summary = :summary, actionItems = :actionItems, keyPoints = :keyPoints, status = :status WHERE meetingId = :meetingId")
    suspend fun updateContent(meetingId: String, title: String, summary: String, actionItems: String, keyPoints: String, status: SummaryStatus)

    @Query("UPDATE summaries SET status = :status, errorMessage = :errorMessage WHERE meetingId = :meetingId")
    suspend fun updateStatus(meetingId: String, status: SummaryStatus, errorMessage: String?)

    @Query("SELECT * FROM summaries WHERE status = :status")
    suspend fun getSummariesByStatus(status: SummaryStatus): List<SummaryEntity>
}
