package com.example.agenttest.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.vector.ImageVector

object SmartContextUtils {
    data class ContextAction(
        val label: String,
        val icon: ImageVector,
        val data: String,
        val type: ActionType
    )

    enum class ActionType {
        URL, EMAIL, PHONE
    }

    private val urlRegex = "\\b(https?://[\\w\\d.-]+(:\\d+)?(/[\\w\\d._/!~*()@%&=+$-]*)?)\\b".toRegex()
    private val emailRegex = "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b".toRegex()
    private val phoneRegex = "\\b(\\+?\\d{1,3}[- ]?)?\\d{10}\\b".toRegex()

    fun extractActions(text: String): List<ContextAction> {
        val actions = mutableListOf<ContextAction>()
        
        urlRegex.find(text)?.let {
            actions.add(ContextAction("Open Link", Icons.Default.Language, it.value, ActionType.URL))
        }
        
        emailRegex.find(text)?.let {
            actions.add(ContextAction("Email", Icons.Default.Email, it.value, ActionType.EMAIL))
        }
        
        phoneRegex.find(text)?.let {
            actions.add(ContextAction("Call", Icons.Default.Phone, it.value, ActionType.PHONE))
        }
        
        return actions.take(2) // Limit to 2 for UI cleanliness
    }
}