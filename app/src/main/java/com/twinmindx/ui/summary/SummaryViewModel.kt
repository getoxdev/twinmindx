package com.twinmindx.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmindx.data.db.entity.SummaryStatus
import com.twinmindx.data.repository.SummaryRepository
import com.twinmindx.domain.model.Summary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val _meetingId = MutableStateFlow<String?>(null)

    fun loadSummary(meetingId: String) {
        _meetingId.value = meetingId
    }

    fun getSummaryFlow(meetingId: String): StateFlow<Summary?> =
        summaryRepository.observeSummary(meetingId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun retryGeneration(meetingId: String) {
        viewModelScope.launch {
            summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.PENDING)
            summaryRepository.enqueueSummaryGeneration(meetingId)
        }
    }
}
