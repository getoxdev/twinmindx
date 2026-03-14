package com.varaha.twinmindx.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class SummaryStatus {
    PENDING, STREAMING, COMPLETED, ERROR
}

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SummaryEntity(
    @PrimaryKey val meetingId: String,
    val title: String = "",
    val summary: String = "",
    val actionItems: String = "",
    val keyPoints: String = "",
    val status: SummaryStatus = SummaryStatus.PENDING,
    val errorMessage: String? = null
)
