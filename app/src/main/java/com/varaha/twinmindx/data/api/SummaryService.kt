package com.varaha.twinmindx.data.api

import kotlinx.coroutines.flow.Flow

data class SummaryChunk(
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null
)

interface SummaryService {
    fun summarize(transcript: String): Flow<SummaryChunk>
}
