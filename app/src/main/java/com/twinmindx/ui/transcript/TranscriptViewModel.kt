package com.twinmindx.ui.transcript

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.data.db.entity.TranscriptChunkEntity
import com.twinmindx.data.repository.RecordingRepository
import com.twinmindx.data.repository.TranscriptionRepository
import com.twinmindx.domain.model.Meeting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranscriptUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isRetrying: Boolean = false
)

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository
) : ViewModel() {

    private val meetingId: String = checkNotNull(savedStateHandle["meetingId"])

    val meeting: StateFlow<Meeting?> = recordingRepository.observeMeeting(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transcriptChunks: StateFlow<List<TranscriptChunkEntity>> =
        transcriptionRepository.observeTranscriptForMeeting(meetingId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState: StateFlow<TranscriptUiState> = _uiState

    fun retryAllChunks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRetrying = true)
            try {
                // Reset meeting to TRANSCRIBING before retrying
                recordingRepository.updateMeetingStatus(meetingId, MeetingStatus.TRANSCRIBING)
                transcriptionRepository.retryAllChunks(meetingId)
            } finally {
                _uiState.value = _uiState.value.copy(isRetrying = false)
            }
        }
    }
}
