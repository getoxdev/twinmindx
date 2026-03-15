package com.twinmindx.data.local

enum class ChunkStatus {
    PENDING, TRANSCRIBING, DONE, FAILED
}

enum class MeetingStatus {
    RECORDING, PAUSED, STOPPED, TRANSCRIBING, SUMMARIZING, COMPLETED, ERROR
}

enum class SummaryStatus {
    PENDING, GENERATING, COMPLETED, ERROR
}