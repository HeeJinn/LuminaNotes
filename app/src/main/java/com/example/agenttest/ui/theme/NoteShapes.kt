package com.example.agenttest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object NoteShapes {
    @Composable
    fun getShapeForColor(color: Color): Shape {
        return when (color.value) {
            0xFFF28B82.toULong() -> RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 4.dp, bottomStart = 4.dp) // Red - Urgent/Sharp
            0xFFFBBC04.toULong() -> RoundedCornerShape(16.dp) // Orange
            0xFFFFF475.toULong() -> RoundedCornerShape(50) // Yellow - Idea/Circular
            0xFFCCFF90.toULong() -> RoundedCornerShape(topStart = 4.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 4.dp) // Green - Growth/Leaf
            0xFFA7FFEB.toULong() -> RoundedCornerShape(8.dp) // Teal
            else -> MaterialTheme.shapes.extraLarge // Default
        }
    }

    @Composable
    fun getTextStyleForColor(color: Color, baseStyle: TextStyle): TextStyle {
        return when (color.value) {
            0xFFF28B82.toULong() -> baseStyle.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold) // Urgent
            0xFFFFF475.toULong(), 0xFFFBBC04.toULong() -> baseStyle.copy(fontFamily = FontFamily.Serif) // Creative
            0xFFCCFF90.toULong() -> baseStyle.copy(fontFamily = FontFamily.Monospace) // Structured
            else -> baseStyle
        }
    }
}
