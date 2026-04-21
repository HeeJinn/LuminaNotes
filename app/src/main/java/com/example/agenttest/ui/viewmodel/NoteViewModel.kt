package com.example.agenttest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agenttest.data.local.dao.NoteDao
import com.example.agenttest.data.local.entity.NoteEntity
import com.example.agenttest.util.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NoteFilter { ALL, PINNED, ARCHIVED, DELETED }

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteDao: NoteDao,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val allLabels: StateFlow<List<String>> = noteDao.getAllNotes()
        .combine(noteDao.getDeletedNotes()) { active, deleted ->
            (active + deleted).flatMap { it.labels }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<NoteEntity>> = combine(
        noteDao.getAllNotes(),
        noteDao.getDeletedNotes(),
        _searchQuery,
        _currentFilter,
        _selectedLabel
    ) { activeNotes, deletedNotes, query, filter, label ->
        _isLoading.value = false 
        
        val sourceList = if (filter == NoteFilter.DELETED) deletedNotes else activeNotes
        
        if (sourceList.isEmpty()) return@combine emptyList<NoteEntity>()

        val filtered = if (query.isEmpty() && filter == NoteFilter.ALL && label == null) {
            sourceList.filter { !it.isArchived }
        } else {
            sourceList.filter { note ->
                val matchesQuery = if (query.isEmpty()) true else {
                    note.title.contains(query, ignoreCase = true) ||
                            com.example.agenttest.util.NoteMetadataUtils.stripHtml(note.content).contains(query, ignoreCase = true)
                }
                val matchesFilter = when (filter) {
                    NoteFilter.ALL -> !note.isArchived
                    NoteFilter.PINNED -> note.isPinned && !note.isArchived
                    NoteFilter.ARCHIVED -> note.isArchived
                    NoteFilter.DELETED -> true
                }
                val matchesLabel = if (label == null) true else note.labels.contains(label)

                matchesQuery && matchesFilter && matchesLabel
            }
        }

        // Sort: Pinned first (if not in PINNED or DELETED filter), then by date descending
        filtered.sortedWith(
            compareByDescending<NoteEntity> { if (filter == NoteFilter.DELETED) false else it.isPinned }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
        _selectedLabel.value = null // Reset label filter when changing main filter
    }

    fun setSelectedLabel(label: String?) {
        _selectedLabel.value = label
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

    fun saveNote(
        id: String?,
        title: String,
        content: String,
        type: com.example.agenttest.data.local.entity.NoteType = com.example.agenttest.data.local.entity.NoteType.TEXT,
        checklistItems: List<com.example.agenttest.data.local.entity.ChecklistItem> = emptyList(),
        color: Int? = null,
        label: String? = null,
        reminderTime: Long? = null,
        labels: List<String> = emptyList(),
        isLocked: Boolean? = null,
        isPinned: Boolean? = null
    ) {
        viewModelScope.launch {
            val existingNote = id?.let { noteDao.getNoteById(it) }
            val note = if (existingNote != null) {
                existingNote.copy(
                    title = title,
                    content = content,
                    type = type,
                    checklistItems = checklistItems,
                    color = color ?: existingNote.color,
                    label = label ?: existingNote.label,
                    reminderTime = reminderTime ?: existingNote.reminderTime,
                    labels = if (labels.isNotEmpty()) labels else existingNote.labels,
                    isLocked = isLocked ?: existingNote.isLocked,
                    isPinned = isPinned ?: existingNote.isPinned,
                    createdAt = System.currentTimeMillis() // Update last modified time
                )
            } else {
                NoteEntity(
                    title = title,
                    content = content,
                    type = type,
                    checklistItems = checklistItems,
                    color = color ?: 0xFFFFFFFF.toInt(),
                    label = label,
                    reminderTime = reminderTime,
                    labels = labels,
                    isLocked = isLocked ?: false,
                    isPinned = isPinned ?: false
                )
            }
            noteDao.insertNote(note)
            
            // Handle reminders
            if (reminderTime != null) {
                reminderManager.setReminder(note, reminderTime)
            } else if (existingNote?.reminderTime != null && id != null) {
                reminderManager.cancelReminder(id)
            }
        }
    }

    fun softDeleteNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                noteDao.softDeleteNote(noteId, System.currentTimeMillis())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                noteDao.restoreNote(noteId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotePermanently(note: NoteEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                noteDao.deleteNote(note)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteDao.deleteAllDeletedNotes()
        }
    }

    fun setReminder(note: NoteEntity, timeInMillis: Long) {
        viewModelScope.launch {
            val updatedNote = note.copy(reminderTime = timeInMillis)
            noteDao.insertNote(updatedNote)
            reminderManager.setReminder(updatedNote, timeInMillis)
        }
    }

    fun cancelReminder(note: NoteEntity) {
        viewModelScope.launch {
            val updatedNote = note.copy(reminderTime = null)
            noteDao.insertNote(updatedNote)
            reminderManager.cancelReminder(note.id)
        }
    }

    fun toggleLabel(note: NoteEntity, label: String) {
        viewModelScope.launch {
            val newLabels = if (note.labels.contains(label)) {
                note.labels.filter { it != label }
            } else {
                note.labels + label
            }
            noteDao.insertNote(note.copy(labels = newLabels))
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isLocked = !note.isLocked))
        }
    }
}