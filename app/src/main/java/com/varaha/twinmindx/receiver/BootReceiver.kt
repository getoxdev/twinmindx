package com.varaha.twinmindx.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.varaha.twinmindx.data.db.entity.SummaryStatus
import com.varaha.twinmindx.data.repository.SummaryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var summaryRepository: SummaryRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val incompleteSummaries = summaryRepository.getIncompleteSummaries()
                    incompleteSummaries.forEach { summary ->
                        summaryRepository.updateSummaryStatus(
                            meetingId = summary.meetingId,
                            status = SummaryStatus.PENDING
                        )
                        summaryRepository.enqueueSummaryGeneration(summary.meetingId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
