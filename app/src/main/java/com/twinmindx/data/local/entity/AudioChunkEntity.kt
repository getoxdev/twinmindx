package com.twinmindx.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.twinmindx.data.local.ChunkStatus

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId")]
)
data class AudioChunkEntity(
    @PrimaryKey val id: String,
    val meetingId: String,
    val chunkIndex: Int,
    val filePath: String,
    val durationMs: Long,
    val status: ChunkStatus = ChunkStatus.PENDING,
    val createdAtMs: Long = System.currentTimeMillis()
)
