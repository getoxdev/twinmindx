package com.twinmindx.ui.recording

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmindx.data.repository.RecordingRepository
import com.twinmindx.domain.models.Meeting
import com.twinmindx.service.RecordingService
import com.twinmindx.service.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private var recordingService: RecordingService? = null
    private var isBound = false

    private val _meetingId = MutableStateFlow<String?>(null)
    val meetingId: StateFlow<String?> = _meetingId.asStateFlow()

    val meeting: StateFlow<Meeting?> = _meetingId.flatMapLatest { id ->
        id?.let { recordingRepository.observeMeeting(it) } ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.LocalBinder
            recordingService = binder.getService()
            isBound = true

            viewModelScope.launch {
                recordingService?.recordingState?.collect { state ->
                    _recordingState.value = state
                }
            }
            viewModelScope.launch {
                recordingService?.statusMessage?.collect { msg ->
                    _statusMessage.value = msg
                }
            }
            viewModelScope.launch {
                recordingService?.elapsedTime?.collect { seconds ->
                    _elapsedSeconds.value = seconds
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            isBound = false
        }
    }

    fun bindToService(meetingId: String) {
        _meetingId.value = meetingId
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopRecording() {
        viewModelScope.launch {
            recordingService?.stopRecording()
        }
    }

    fun pauseRecording() {
        recordingService?.userPauseRecording()
    }

    fun resumeRecording() {
        recordingService?.userResumeRecording()
    }

    fun formatElapsedTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun isMeetingFinished(): Boolean {
        return _recordingState.value == RecordingState.STOPPED
    }
}
