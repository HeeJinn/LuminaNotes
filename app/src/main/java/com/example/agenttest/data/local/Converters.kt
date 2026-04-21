package com.example.agenttest.data.local

import androidx.room.TypeConverter
import com.example.agenttest.data.local.entity.ChecklistItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromChecklistItemList(value: List<ChecklistItem>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toChecklistItemList(value: String): List<ChecklistItem> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}