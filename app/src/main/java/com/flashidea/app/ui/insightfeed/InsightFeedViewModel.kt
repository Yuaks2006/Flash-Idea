package com.flashidea.app.ui.insightfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.repository.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightFeedViewModel @Inject constructor(private val repository: IdeaRepository) : ViewModel() {
    val insights: StateFlow<List<InsightEntity>> = repository.getAllInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAllRead() { viewModelScope.launch { repository.markAllInsightsRead() } }
}
