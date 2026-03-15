package com.twinmindx.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.twinmindx.data.db.dao.SummaryDao
import com.twinmindx.data.db.entity.SummaryEntity
import com.twinmindx.data.db.entity.SummaryStatus
import com.twinmindx.domain.models.Summary
import com.twinmindx.domain.models.toDomain
import com.twinmindx.worker.SummaryWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
    private val workManager: WorkManager
) {

    fun observeSummary(meetingId: String): Flow<Summary?> =
        summaryDao.observeSummary(meetingId).map { it?.toDomain() }

    suspend fun getSummary(meetingId: String): Summary? =
        summaryDao.getSummary(meetingId)?.toDomain()

    suspend fun enqueueSummaryGeneration(meetingId: String) {
        val now = System.currentTimeMillis()
        val existing = summaryDao.getSummary(meetingId)
        if (existing == null) {
            summaryDao.insert(
                SummaryEntity(
                    meetingId = meetingId,
                    status = SummaryStatus.PENDING,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            )
        } else {
            summaryDao.updateStatus(meetingId, SummaryStatus.PENDING, now)
        }

        workManager.enqueueUniqueWork(
            SummaryWorker.workName(meetingId),
            ExistingWorkPolicy.KEEP,
            buildWorkRequest(meetingId)
        )
    }

    suspend fun retrySummaryGeneration(meetingId: String) {
        val now = System.currentTimeMillis()
        val existing = summaryDao.getSummary(meetingId)
        if (existing != null) {
            summaryDao.updateError(meetingId, SummaryStatus.PENDING, null, now)
        } else {
            summaryDao.insert(
                SummaryEntity(
                    meetingId = meetingId,
                    status = SummaryStatus.PENDING,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            )
        }

        workManager.enqueueUniqueWork(
            SummaryWorker.workName(meetingId),
            ExistingWorkPolicy.REPLACE,
            buildWorkRequest(meetingId)
        )
    }

    private fun buildWorkRequest(meetingId: String) =
        OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(
                Data.Builder()
                    .putString(SummaryWorker.KEY_MEETING_ID, meetingId)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
}
