package com.example.agenttest.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object ColorUtils {
    fun getContrastingColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            Color(0xFF1C1B1F) // Dark grey/black for light backgrounds
        } else {
            Color.White // White for dark backgrounds
        }
    }
    
    fun getSecondaryContrastingColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            Color(0xFF1C1B1F).copy(alpha = 0.7f)
        } else {
            Color.White.copy(alpha = 0.7f)
        }
    }
}