package com.example.agenttest.util

object NoteMetadataUtils {
    fun getWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun getReadingTimeMinutes(text: String): Int {
        val words = getWordCount(text)
        return (words / 200).coerceAtLeast(1)
    }

    enum class GrowthStage {
        SEED, SPROUT, BLOSSOM
    }

    fun getGrowthStage(text: String): GrowthStage {
        val length = text.length
        return when {
            length < 50 -> GrowthStage.SEED
            length < 200 -> GrowthStage.SPROUT
            else -> GrowthStage.BLOSSOM
        }
    }

    fun suggestColor(title: String, content: String): Int? {
        val combined = (title + " " + content).lowercase()
        return when {
            combined.contains("urgent") || combined.contains("deadline") || combined.contains("important") -> 0xFFF28B82.toInt() // Red
            combined.contains("buy") || combined.contains("shop") || combined.contains("money") -> 0xFFCCFF90.toInt() // Green
            combined.contains("idea") || combined.contains("think") || combined.contains("creative") -> 0xFFFFF475.toInt() // Yellow
            combined.contains("work") || combined.contains("meeting") || combined.contains("project") -> 0xFFAECBFA.toInt() // Blue
            else -> null
        }
    }
}