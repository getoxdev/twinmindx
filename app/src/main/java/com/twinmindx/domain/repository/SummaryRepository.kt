package com.twinmindx.domain.repository

import com.twinmindx.domain.models.Summary
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    fun observeSummary(meetingId: String): Flow<Summary?>
    suspend fun getSummary(meetingId: String): Summary?
    suspend fun enqueueSummaryGeneration(meetingId: String)
    suspend fun retrySummaryGeneration(meetingId: String)
}
