package com.example.agenttest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.agenttest.data.local.dao.NoteDao
import com.example.agenttest.data.local.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 8)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}