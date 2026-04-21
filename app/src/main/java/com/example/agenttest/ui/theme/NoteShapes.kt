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
            0xFFF28B82.toULong() -> RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 8.dp, bottomStart = 8.dp) // Red - Urgent/Organic
            0xFFFBBC04.toULong() -> RoundedCornerShape(24.dp) // Orange - Softer
            0xFFFFF475.toULong() -> RoundedCornerShape(percent = 40) // Yellow - Rounded/Organic
            0xFFCCFF90.toULong() -> RoundedCornerShape(topStart = 8.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 8.dp) // Green - Leaf/Organic
            0xFFA7FFEB.toULong() -> RoundedCornerShape(16.dp) // Teal - Softer
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
