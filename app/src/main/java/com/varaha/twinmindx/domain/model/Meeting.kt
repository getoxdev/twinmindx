package com.varaha.twinmindx.domain.model

import com.varaha.twinmindx.data.db.entity.MeetingEntity
import com.varaha.twinmindx.data.db.entity.MeetingStatus

data class Meeting(
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val status: MeetingStatus,
    val totalChunks: Int
)

fun MeetingEntity.toDomain() = Meeting(
    id = id,
    title = title,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    status = status,
    totalChunks = totalChunks
)

fun Meeting.toEntity() = MeetingEntity(
    id = id,
    title = title,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    status = status,
    totalChunks = totalChunks
)
