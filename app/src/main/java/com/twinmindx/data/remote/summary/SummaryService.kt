package com.twinmindx.data.remote.summary

import com.twinmindx.domain.models.SummaryResult
import kotlinx.coroutines.flow.Flow

interface SummaryService {
    fun streamSummary(transcript: String): Flow<String>
    fun parseSummaryResult(rawJson: String): SummaryResult
}
