package com.twinmindx.data.local

import androidx.room.TypeConverter
import com.twinmindx.data.local.entity.ChunkStatus
import com.twinmindx.data.local.entity.MeetingStatus
import com.twinmindx.data.local.entity.SummaryStatus

class Converters {

    @TypeConverter
    fun fromMeetingStatus(status: MeetingStatus): String = status.name

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus = MeetingStatus.valueOf(value)

    @TypeConverter
    fun fromChunkStatus(status: ChunkStatus): String = status.name

    @TypeConverter
    fun toChunkStatus(value: String): ChunkStatus = ChunkStatus.valueOf(value)

    @TypeConverter
    fun fromSummaryStatus(status: SummaryStatus): String = status.name

    @TypeConverter
    fun toSummaryStatus(value: String): SummaryStatus = SummaryStatus.valueOf(value)
}
