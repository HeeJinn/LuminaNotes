package com.example.agenttest.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object NoteMetadataUtils {
    fun getWordCount(content: String): Int {
        val text = stripHtml(content)
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun getReadingTimeMinutes(content: String): Int {
        val words = getWordCount(content)
        return (words / 200).coerceAtLeast(1)
    }

    fun getCharacterCount(content: String): Int {
        return stripHtml(content).length
    }

    enum class GrowthStage {
        SEED, SPROUT, BLOSSOM
    }

    fun getGrowthStage(content: String): GrowthStage {
        val stripped = stripHtml(content)
        val length = stripped.length
        val hasMedia = content.contains("audio:") || content.contains("image:")
        return when {
            length < 50 && !hasMedia -> GrowthStage.SEED
            length < 200 && !hasMedia -> GrowthStage.SPROUT
            else -> GrowthStage.BLOSSOM
        }
    }

    fun suggestColor(title: String, content: String): Int? {
        val combined = (title + " " + stripHtml(content)).lowercase()
        return when {
            combined.contains("urgent") || combined.contains("deadline") || combined.contains("important") -> 0xFFF28B82.toInt() // Red
            combined.contains("buy") || combined.contains("shop") || combined.contains("money") -> 0xFFCCFF90.toInt() // Green
            combined.contains("idea") || combined.contains("think") || combined.contains("creative") -> 0xFFFFF475.toInt() // Yellow
            combined.contains("work") || combined.contains("meeting") || combined.contains("project") -> 0xFFAECBFA.toInt() // Blue
            combined.contains("gym") || combined.contains("health") || combined.contains("exercise") -> 0xFFA7FFEB.toInt() // Teal
            combined.contains("gift") || combined.contains("birthday") || combined.contains("party") -> 0xFFD7AEFB.toInt() // Purple
            else -> null
        }
    }

    fun getSentimentIcon(content: String): String? {
        val text = stripHtml(content).lowercase()
        return when {
            text.contains("happy") || text.contains("great") || text.contains("awesome") || text.contains("love") -> "Sparkles"
            text.contains("sad") || text.contains("bad") || text.contains("unhappy") || text.contains("sorry") -> "Cloud"
            text.contains("angry") || text.contains("mad") || text.contains("hate") -> "Fire"
            text.contains("think") || text.contains("idea") || text.contains("maybe") -> "Lightbulb"
            text.contains("check") || text.contains("done") || text.contains("finish") -> "Done"
            else -> null
        }
    }

    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        // Replace media tags with descriptive text for preview
        var processed = html
        // Look for audio: or image: links
        processed = processed.replace("<a[^>]*audio:[^>]*>(.*?)</a>".toRegex(), " [Voice Memo] ")
        processed = processed.replace("<a[^>]*image:[^>]*>(.*?)</a>".toRegex(), " [Sketch] ")
        
        return processed.replace("<[^>]*>".toRegex(), " ")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    fun getHighlightedText(
        text: String,
        query: String,
        highlightColor: Color
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)
        
        return buildAnnotatedString {
            var start = 0
            while (start < text.length) {
                val index = text.indexOf(query, start, ignoreCase = true)
                if (index == -1) {
                    append(text.substring(start))
                    break
                }
                
                append(text.substring(start, index))
                withStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(index, index + query.length))
                }
                start = index + query.length
            }
        }
    }
}
