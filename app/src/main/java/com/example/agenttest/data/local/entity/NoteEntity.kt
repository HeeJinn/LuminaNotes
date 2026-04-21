package com.example.agenttest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

enum class NoteType { TEXT, CHECKLIST }

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val type: NoteType = NoteType.TEXT,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val color: Int = 0xFFFFFFFF.toInt(), // Default white
    val label: String? = null,
    val reminderTime: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val labels: List<String> = emptyList(),
    val isLocked: Boolean = false
)