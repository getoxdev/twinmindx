package com.twinmindx

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.twinmindx.worker.TerminationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TwinMindApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enqueueTerminationWorker()
    }

    private fun enqueueTerminationWorker() {
        val request = OneTimeWorkRequestBuilder<TerminationWorker>()
            .addTag(TerminationWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            TerminationWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
