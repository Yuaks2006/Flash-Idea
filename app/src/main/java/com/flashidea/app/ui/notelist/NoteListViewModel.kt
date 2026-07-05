package com.flashidea.app.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.repository.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: IdeaRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val ideas: StateFlow<List<IdeaEntity>> = searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAllIdeas()
            else repository.searchIdeas(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(q: String) { searchQuery.value = q }
}
