package com.twinmindx.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_chunks",
    foreignKeys = [
        ForeignKey(
            entity = AudioChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioChunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("audioChunkId"), Index("meetingId")]
)
data class TranscriptChunkEntity(
    @PrimaryKey val id: String,
    val meetingId: String,
    val audioChunkId: String,
    val chunkIndex: Int,
    val text: String,
    val createdAtMs: Long = System.currentTimeMillis()
)
