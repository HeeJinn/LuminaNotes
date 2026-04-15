package com.example.agenttest.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val noteDate = Calendar.getInstance().apply { time = date }

        return if (now.get(Calendar.YEAR) == noteDate.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == noteDate.get(Calendar.DAY_OF_YEAR)) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    }
}