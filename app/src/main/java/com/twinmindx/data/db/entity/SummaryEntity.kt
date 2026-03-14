package com.twinmindx.data.db.entity

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
    /** JSON-encoded list of strings */
    val actionItems: String? = null,
    /** JSON-encoded list of strings */
    val keyPoints: String? = null,
    /** Accumulates raw streaming text while generation is in progress */
    val streamingText: String? = null,
    val status: SummaryStatus = SummaryStatus.PENDING,
    val errorMessage: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
