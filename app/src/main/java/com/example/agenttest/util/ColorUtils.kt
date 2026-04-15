package com.example.agenttest.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object ColorUtils {
    fun getContrastingColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.4f) {
            Color.Black
        } else {
            Color.White
        }
    }
    
    fun getSecondaryContrastingColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.4f) {
            Color.Black.copy(alpha = 0.6f)
        } else {
            Color.White.copy(alpha = 0.7f)
        }
    }
}