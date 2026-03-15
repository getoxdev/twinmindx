package com.twinmindx.domain.models

import com.twinmindx.data.db.entity.MeetingEntity
import com.twinmindx.data.db.entity.MeetingStatus

data class Meeting(
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val status: MeetingStatus,
)

fun MeetingEntity.toDomain() = Meeting(
    id = id,
    title = title,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    status = status,
)

fun Meeting.toEntity() = MeetingEntity(
    id = id,
    title = title,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    status = status,
)
