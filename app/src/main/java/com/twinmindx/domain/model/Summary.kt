package com.twinmindx.domain.model

import com.twinmindx.data.db.entity.SummaryEntity
import com.twinmindx.data.db.entity.SummaryStatus

data class Summary(
    val meetingId: String,
    val title: String,
    val summary: String,
    val actionItems: String,
    val keyPoints: String,
    val status: SummaryStatus,
    val errorMessage: String?
)

fun SummaryEntity.toDomain() = Summary(
    meetingId = meetingId,
    title = title,
    summary = summary,
    actionItems = actionItems,
    keyPoints = keyPoints,
    status = status,
    errorMessage = errorMessage
)

fun Summary.toEntity() = SummaryEntity(
    meetingId = meetingId,
    title = title,
    summary = summary,
    actionItems = actionItems,
    keyPoints = keyPoints,
    status = status,
    errorMessage = errorMessage
)
