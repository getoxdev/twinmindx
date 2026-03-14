package com.varaha.twinmindx.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MeetingStatus {
    RECORDING, PAUSED, STOPPED, TRANSCRIBING, SUMMARIZING, COMPLETED, ERROR
}

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val status: MeetingStatus,
    val totalChunks: Int = 0
)
