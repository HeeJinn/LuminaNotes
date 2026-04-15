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

    fun getMoodEmoji(text: String): String {
        val content = text.lowercase()
        val positiveWords = listOf("happy", "good", "great", "awesome", "love", "smile", "fun", "excited", "best")
        val negativeWords = listOf("sad", "bad", "angry", "hate", "terrible", "worst", "cry", "upset", "sorry")
        val taskWords = listOf("todo", "task", "buy", "remember", "clean", "fix", "work", "project", "deadline")

        val positiveCount = positiveWords.count { content.contains(it) }
        val negativeCount = negativeWords.count { content.contains(it) }
        val taskCount = taskWords.count { content.contains(it) }

        return when {
            taskCount > positiveCount && taskCount > negativeCount -> "📝"
            positiveCount > negativeCount -> "✨"
            negativeCount > positiveCount -> "☁️"
            else -> "📄"
        }
    }
}