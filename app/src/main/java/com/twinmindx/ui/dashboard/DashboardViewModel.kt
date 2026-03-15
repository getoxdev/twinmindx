package com.twinmindx.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmindx.domain.repositories.RecordingRepository
import com.twinmindx.domain.models.Meeting
import com.twinmindx.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val meetings: StateFlow<List<Meeting>> = recordingRepository.getAllMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startNewRecording(onMeetingCreated: (String) -> Unit) {
        viewModelScope.launch {
            val meetingId = recordingRepository.createMeeting()
            val intent = Intent(context, RecordingService::class.java).apply {
                putExtra(RecordingService.EXTRA_MEETING_ID, meetingId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            onMeetingCreated(meetingId)
        }
    }
}
