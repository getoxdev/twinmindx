package com.twinmindx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SummaryStatus {
    PENDING, GENERATING, COMPLETED, ERROR
}

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey val meetingId: String,
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null,
    val streamingText: String? = null,
    val status: SummaryStatus = SummaryStatus.PENDING,
    val errorMessage: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
