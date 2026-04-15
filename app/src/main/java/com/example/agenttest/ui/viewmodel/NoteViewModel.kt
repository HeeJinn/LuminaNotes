package com.example.agenttest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agenttest.data.local.dao.NoteDao
import com.example.agenttest.data.local.entity.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NoteFilter { ALL, PINNED, ARCHIVED }

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter

    val notes: StateFlow<List<NoteEntity>> = combine(
        noteDao.getAllNotes(),
        _searchQuery,
        _currentFilter
    ) { notes, query, filter ->
        val filtered = notes.filter { note ->
            val matchesQuery = note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                NoteFilter.ALL -> !note.isArchived
                NoteFilter.PINNED -> note.isPinned && !note.isArchived
                NoteFilter.ARCHIVED -> note.isArchived
            }
            matchesQuery && matchesFilter
        }
        // Sort: Pinned first (if not in PINNED filter), then by date descending
        filtered.sortedWith(
            compareByDescending<NoteEntity> { it.isPinned }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isArchived = !note.isArchived))
        }
    }

    fun updateNoteColor(note: NoteEntity, color: Int) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(color = color))
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getNoteById(id: String, onResult: (NoteEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(noteDao.getNoteById(id))
        }
    }

    fun saveNote(id: String?, title: String, content: String, color: Int? = null, label: String? = null) {
        viewModelScope.launch {
            val existingNote = id?.let { noteDao.getNoteById(it) }
            val note = if (existingNote != null) {
                existingNote.copy(
                    title = title,
                    content = content,
                    color = color ?: existingNote.color,
                    label = label ?: existingNote.label,
                    createdAt = System.currentTimeMillis() // Update last modified time
                )
            } else {
                NoteEntity(
                    title = title,
                    content = content,
                    color = color ?: 0xFFFFFFFF.toInt(),
                    label = label
                )
            }
            noteDao.insertNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun insertNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.insertNote(note)
        }
    }
}