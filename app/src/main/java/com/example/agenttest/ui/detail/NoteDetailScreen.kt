package com.example.agenttest.ui.detail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agenttest.ui.home.DarkNoteColors
import com.example.agenttest.ui.home.NoteColors
import com.example.agenttest.ui.viewmodel.NoteViewModel
import com.example.agenttest.util.NoteMetadataUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String? = null,
    onBackClick: () -> Unit,
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()
    val note = notes.find { it.id == noteId }

    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var color by remember { mutableStateOf(note?.color ?: 0xFFFFFFFF.toInt()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val hasChanges = title != (note?.title ?: "") || content != (note?.content ?: "") || color != (note?.color ?: 0xFFFFFFFF.toInt())

    // Auto-suggest color based on content
    LaunchedEffect(title, content) {
        if (noteId == null && color == 0xFFFFFFFF.toInt()) {
            NoteMetadataUtils.suggestColor(title, content)?.let { suggested ->
                color = suggested
            }
        }
    }

    val handleBack = {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                viewModel.saveNote(
                                    noteId,
                                    title,
                                    content,
                                    color = color
                                )
                            }
                            onBackClick()
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            val growthStage = NoteMetadataUtils.getGrowthStage(content)
            val growthInfo = when(growthStage) {
                NoteMetadataUtils.GrowthStage.SEED -> "🌱 Seedling"
                NoteMetadataUtils.GrowthStage.SPROUT -> "🌿 Sprouting"
                NoteMetadataUtils.GrowthStage.BLOSSOM -> "🌸 Blossoming"
            }
            
            Text(
                text = growthInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.displaySmall) },
                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing...", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 10
            )
        }
    }

    if (showColorPicker) {
        ModalBottomSheet(onDismissRequest = { showColorPicker = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Select color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                val colors = if (isSystemInDarkTheme()) DarkNoteColors else NoteColors
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(colors.size) { index ->
                        val itemColor = colors[index]
                        Surface(
                            onClick = { color = itemColor.toArgb(); showColorPicker = false },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = itemColor,
                            modifier = Modifier.size(48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            if (color == itemColor.toArgb()) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
