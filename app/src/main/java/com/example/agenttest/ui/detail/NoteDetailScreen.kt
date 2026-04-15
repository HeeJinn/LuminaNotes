package com.example.agenttest.ui.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
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
    val richTextState = rememberRichTextState()
    
    // Load initial content
    LaunchedEffect(note) {
        if (note != null && richTextState.annotatedString.text.isEmpty()) {
            richTextState.setHtml(note.content)
        }
    }

    var color by remember { mutableStateOf(note?.color ?: 0xFFFFFFFF.toInt()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val hasChanges = title != (note?.title ?: "") || richTextState.toHtml() != (note?.content ?: "") || color != (note?.color ?: 0xFFFFFFFF.toInt())

    // Auto-suggest color based on content
    LaunchedEffect(title, richTextState.annotatedString.text) {
        if (noteId == null && color == 0xFFFFFFFF.toInt()) {
            NoteMetadataUtils.suggestColor(title, richTextState.annotatedString.text)?.let { suggested ->
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
                            if (title.isNotBlank() || richTextState.annotatedString.text.isNotBlank()) {
                                viewModel.saveNote(
                                    noteId,
                                    title,
                                    richTextState.toHtml(),
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
        },
        bottomBar = {
            FloatingToolbar(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding(),
                richTextState = richTextState
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
            val growthStage = NoteMetadataUtils.getGrowthStage(richTextState.annotatedString.text)
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
            
            RichTextEditor(
                state = richTextState,
                placeholder = { Text("Start typing...", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
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

enum class ToolbarAction {
    H1, H2, BOLD, ITALIC, UNDERLINE, LIST, CHECKLIST, IMAGE
}

@Composable
fun FloatingToolbar(
    modifier: Modifier = Modifier,
    richTextState: RichTextState
) {
    val h1Size = MaterialTheme.typography.headlineLarge.fontSize
    val h2Size = MaterialTheme.typography.titleLarge.fontSize

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .height(56.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarButton(
                icon = Icons.Default.Title,
                contentDescription = "Title",
                isActive = false
            ) { richTextState.toggleSpanStyle(SpanStyle(fontSize = h1Size)) }
            
            ToolbarButton(
                icon = Icons.Outlined.Title,
                contentDescription = "Subtitle",
                isActive = false
            ) { richTextState.toggleSpanStyle(SpanStyle(fontSize = h2Size)) }
            
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                isActive = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold
            ) { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                isActive = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic
            ) { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                isActive = richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline
            ) { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "List",
                isActive = false 
            ) { richTextState.toggleUnorderedList() }
            
            ToolbarButton(
                icon = Icons.Default.Checklist,
                contentDescription = "Checklist",
                isActive = false
            ) { }
        }
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
