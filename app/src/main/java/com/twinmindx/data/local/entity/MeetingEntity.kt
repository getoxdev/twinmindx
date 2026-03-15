package com.twinmindx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.twinmindx.data.local.MeetingStatus

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val status: MeetingStatus,
    val totalChunks: Int = 0
)
