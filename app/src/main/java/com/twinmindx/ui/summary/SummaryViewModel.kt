package com.twinmindx.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmindx.data.db.entity.SummaryStatus
import com.twinmindx.domain.models.Summary
import com.twinmindx.domain.repositories.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    val meetingId: String = checkNotNull(savedStateHandle["meetingId"])

    val summary: StateFlow<Summary?> = summaryRepository.observeSummary(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            val existing = summaryRepository.getSummary(meetingId)
            val shouldGenerate = existing == null ||
                existing.status == SummaryStatus.PENDING ||
                existing.status == SummaryStatus.ERROR
            if (shouldGenerate) {
                summaryRepository.enqueueSummaryGeneration(meetingId)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            summaryRepository.retrySummaryGeneration(meetingId)
        }
    }
}
