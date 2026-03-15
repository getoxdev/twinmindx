package com.twinmindx.domain.models

import com.twinmindx.data.local.entity.TranscriptChunkEntity

data class TranscriptChunk(
    val id: String,
    val meetingId: String,
    val audioChunkId: String,
    val chunkIndex: Int,
    val text: String,
    val createdAtMs: Long
)

fun TranscriptChunkEntity.toDomain(): TranscriptChunk {
    return TranscriptChunk(
        id = id,
        meetingId = meetingId,
        audioChunkId = audioChunkId,
        chunkIndex = chunkIndex,
        text = text,
        createdAtMs = createdAtMs
    )
}

fun TranscriptChunk.toEntity(): TranscriptChunkEntity {
    return TranscriptChunkEntity(
        id = id,
        meetingId = meetingId,
        audioChunkId = audioChunkId,
        chunkIndex = chunkIndex,
        text = text,
        createdAtMs = createdAtMs
    )
}
