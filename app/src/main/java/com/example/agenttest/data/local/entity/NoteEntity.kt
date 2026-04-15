package com.example.agenttest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val color: Int = 0xFFFFFFFF.toInt(), // Default white
    val label: String? = null
)