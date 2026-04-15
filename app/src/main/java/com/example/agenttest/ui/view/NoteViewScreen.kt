package com.example.agenttest.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agenttest.ui.theme.NoteShapes
import com.example.agenttest.ui.viewmodel.NoteViewModel
import com.example.agenttest.util.ColorUtils
import com.example.agenttest.util.DateUtils
import com.example.agenttest.util.NoteMetadataUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewScreen(
    noteId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()
    val note = notes.find { it.id == noteId }

    if (note == null) {
        onBackClick()
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isCustomColor = note.color != 0xFFFFFFFF.toInt()
    val backgroundColor = if (isCustomColor) Color(note.color) else MaterialTheme.colorScheme.surface
    val contentColor = if (isCustomColor) ColorUtils.getContrastingColor(backgroundColor) else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = if (isCustomColor) ColorUtils.getSecondaryContrastingColor(backgroundColor) else MaterialTheme.colorScheme.outline
    
    val titleStyle = NoteShapes.getTextStyleForColor(backgroundColor, MaterialTheme.typography.displaySmall)
    val bodyStyle = NoteShapes.getTextStyleForColor(backgroundColor, MaterialTheme.typography.bodyLarge)
    
    val wordCount = NoteMetadataUtils.getWordCount(note.content)
    val readingTime = NoteMetadataUtils.getReadingTimeMinutes(note.content)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = backgroundColor,
            topBar = {
                LargeTopAppBar(
                    title = { 
                        if (scrollBehavior.state.collapsedFraction > 0.5f) {
                            Text(
                                text = note.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleLarge,
                                color = contentColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = contentColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.togglePin(note) }) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = contentColor
                            )
                        }
                        IconButton(onClick = { onEditClick(note.id) }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Edit",
                                tint = contentColor
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Delete",
                                tint = contentColor
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = backgroundColor,
                        scrolledContainerColor = backgroundColor,
                        titleContentColor = contentColor,
                        navigationIconContentColor = contentColor,
                        actionIconContentColor = contentColor
                    )
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
                Text(
                    text = note.title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = contentColor.copy(alpha = 0.05f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MetadataItem(Icons.Default.Timer, "$readingTime min read", secondaryContentColor)
                        MetadataItem(Icons.Default.Abc, "$wordCount words", secondaryContentColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Last edited ${DateUtils.formatTimestamp(note.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryContentColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = note.content,
                    style = bodyStyle.copy(lineHeight = bodyStyle.lineHeight * 1.2),
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently removed.") },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note)
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetadataItem(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
