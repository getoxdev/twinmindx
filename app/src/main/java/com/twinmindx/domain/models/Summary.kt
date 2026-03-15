package com.twinmindx.domain.models

import com.twinmindx.data.db.entity.SummaryEntity
import com.twinmindx.data.db.entity.SummaryStatus
import org.json.JSONArray

data class Summary(
    val meetingId: String,
    val title: String?,
    val summary: String?,
    val actionItems: List<String>,
    val keyPoints: List<String>,
    val streamingText: String?,
    val status: SummaryStatus,
    val errorMessage: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

fun SummaryEntity.toDomain(): Summary {
    return Summary(
        meetingId = meetingId,
        title = title,
        summary = summary,
        actionItems = parseJsonList(actionItems),
        keyPoints = parseJsonList(keyPoints),
        streamingText = streamingText,
        status = status,
        errorMessage = errorMessage,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

fun Summary.toEntity(): SummaryEntity {
    return SummaryEntity(
        meetingId = meetingId,
        title = title,
        summary = summary,
        actionItems = actionItems.toJsonString(),
        keyPoints = keyPoints.toJsonString(),
        streamingText = streamingText,
        status = status,
        errorMessage = errorMessage,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

private fun parseJsonList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { array.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun List<String>.toJsonString(): String {
    val array = JSONArray()
    forEach { array.put(it) }
    return array.toString()
}
