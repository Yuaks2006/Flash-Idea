package com.flashidea.app.ui.notedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.repository.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(private val repository: IdeaRepository) : ViewModel() {
    private val _idea = MutableStateFlow<IdeaEntity?>(null)
    val idea = _idea.asStateFlow()

    private val _relatedIdeas = MutableStateFlow<List<IdeaEntity>>(emptyList())
    val relatedIdeas = _relatedIdeas.asStateFlow()

    private val _deleteConfirm = MutableStateFlow(false)
    val deleteConfirm = _deleteConfirm.asStateFlow()

    fun load(noteId: String) {
        viewModelScope.launch {
            _idea.value = repository.getIdeaById(noteId)
            repository.getLinksForIdea(noteId).collect { links ->
                val relatedIds = links.map { if (it.sourceId == noteId) it.targetId else it.sourceId }
                _relatedIdeas.value = relatedIds.mapNotNull { repository.getIdeaById(it) }
            }
        }
    }

    /** UI 触发：弹出删除二次确认弹窗。 */
    fun showDeleteConfirm() { _deleteConfirm.value = true }

    /** UI 触发：取消删除，关闭弹窗。 */
    fun dismissDeleteConfirm() { _deleteConfirm.value = false }

    /** UI 触发：用户确认删除，真正执行并回调导航。 */
    fun confirmDelete(onDone: () -> Unit) {
        _deleteConfirm.value = false
        viewModelScope.launch {
            _idea.value?.id?.let { repository.deleteIdea(it) }
            onDone()
        }
    }
}
