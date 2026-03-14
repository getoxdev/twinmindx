package com.varaha.twinmindx.data.repository

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.varaha.twinmindx.data.db.dao.SummaryDao
import com.varaha.twinmindx.data.db.entity.SummaryEntity
import com.varaha.twinmindx.data.db.entity.SummaryStatus
import com.varaha.twinmindx.domain.model.Summary
import com.varaha.twinmindx.domain.model.toDomain
import com.varaha.twinmindx.worker.SummaryWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
    private val workManager: WorkManager
) {

    fun enqueueSummaryGeneration(meetingId: String) {
        val inputData = Data.Builder()
            .putString(SummaryWorker.KEY_MEETING_ID, meetingId)
            .build()

        val request = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(inputData)
            .addTag("summary_$meetingId")
            .build()

        workManager.enqueueUniqueWork(
            "summary_$meetingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun observeSummary(meetingId: String): Flow<Summary?> =
        summaryDao.observeSummaryForMeeting(meetingId).map { it?.toDomain() }

    suspend fun getSummary(meetingId: String): Summary? =
        summaryDao.getSummaryForMeeting(meetingId)?.toDomain()

    suspend fun initializeSummary(meetingId: String) {
        val existing = summaryDao.getSummaryForMeeting(meetingId)
        if (existing == null) {
            summaryDao.insert(SummaryEntity(meetingId = meetingId, status = SummaryStatus.PENDING))
        }
    }

    suspend fun updateSummaryContent(
        meetingId: String,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        status: SummaryStatus
    ) {
        summaryDao.updateContent(meetingId, title, summary, actionItems, keyPoints, status)
    }

    suspend fun updateSummaryStatus(meetingId: String, status: SummaryStatus, errorMessage: String? = null) {
        summaryDao.updateStatus(meetingId, status, errorMessage)
    }

    suspend fun getIncompleteSummaries(): List<SummaryEntity> =
        summaryDao.getSummariesByStatus(SummaryStatus.PENDING) +
        summaryDao.getSummariesByStatus(SummaryStatus.STREAMING)
}
