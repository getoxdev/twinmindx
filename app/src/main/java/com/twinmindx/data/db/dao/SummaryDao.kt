package com.twinmindx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twinmindx.data.db.entity.SummaryEntity
import com.twinmindx.data.db.entity.SummaryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun observeSummary(meetingId: String): Flow<SummaryEntity?>

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummary(meetingId: String): SummaryEntity?

    @Query("UPDATE summaries SET streamingText = :text, updatedAtMs = :updatedAtMs WHERE meetingId = :meetingId")
    suspend fun updateStreamingText(meetingId: String, text: String, updatedAtMs: Long)

    @Query("""
        UPDATE summaries 
        SET title = :title, summary = :summary, actionItems = :actionItems, keyPoints = :keyPoints,
            status = :status, streamingText = NULL, updatedAtMs = :updatedAtMs
        WHERE meetingId = :meetingId
    """)
    suspend fun updateCompleted(
        meetingId: String,
        title: String?,
        summary: String?,
        actionItems: String?,
        keyPoints: String?,
        status: SummaryStatus,
        updatedAtMs: Long
    )

    @Query("UPDATE summaries SET status = :status, errorMessage = :errorMessage, updatedAtMs = :updatedAtMs WHERE meetingId = :meetingId")
    suspend fun updateError(
        meetingId: String,
        status: SummaryStatus,
        errorMessage: String?,
        updatedAtMs: Long
    )

    @Query("UPDATE summaries SET status = :status, updatedAtMs = :updatedAtMs WHERE meetingId = :meetingId")
    suspend fun updateStatus(
        meetingId: String,
        status: SummaryStatus,
        updatedAtMs: Long
    )
}
