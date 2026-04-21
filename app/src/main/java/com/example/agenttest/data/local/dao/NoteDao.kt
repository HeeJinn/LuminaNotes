package com.example.agenttest.data.local.dao

import androidx.room.*
import com.example.agenttest.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNotesOnce(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun deleteAllDeletedNotes()

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :timestamp WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :noteId")
    suspend fun restoreNote(noteId: String)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :timestamp")
    suspend fun purgeOldDeletedNotes(timestamp: Long)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderTime IS NOT NULL")
    suspend fun getNotesWithReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}