package com.example.agenttest.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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

    // Undo/Redo History State
    val undoStack = remember { mutableStateListOf<Pair<String, String>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, String>>() }
    var isInternalUpdate by remember { mutableStateOf(false) }

    fun pushToHistory(newTitle: String, newContent: String) {
        if (isInternalUpdate) return
        
        val lastTitle = if (undoStack.isEmpty()) note?.title ?: "" else undoStack.last().first
        val lastContent = if (undoStack.isEmpty()) note?.content ?: "" else undoStack.last().second
        
        if (newTitle != lastTitle || newContent != lastContent) {
            undoStack.add(lastTitle to lastContent)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isInternalUpdate = true
            val currentState = title to richTextState.toHtml()
            redoStack.add(currentState)
            val lastState = undoStack.removeAt(undoStack.size - 1)
            title = lastState.first
            richTextState.setHtml(lastState.second)
            isInternalUpdate = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isInternalUpdate = true
            val currentState = title to richTextState.toHtml()
            undoStack.add(currentState)
            val nextState = redoStack.removeAt(redoStack.size - 1)
            title = nextState.first
            richTextState.setHtml(nextState.second)
            isInternalUpdate = false
        }
    }

    // Observe changes for history
    LaunchedEffect(title) {
        pushToHistory(title, richTextState.toHtml())
    }

    LaunchedEffect(richTextState.annotatedString) {
        // Debounce to avoid spamming the history with every character
        kotlinx.coroutines.delay(500)
        pushToHistory(title, richTextState.toHtml())
    }
    
    // Load initial content
    LaunchedEffect(note) {
        if (note != null && richTextState.annotatedString.text.isEmpty()) {
            isInternalUpdate = true
            richTextState.setHtml(note.content)
            isInternalUpdate = false
        }
    }

    var color by remember { mutableIntStateOf(note?.color ?: 0xFFFFFFFF.toInt()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val hasChanges by remember {
        derivedStateOf {
            title != (note?.title ?: "") || 
            richTextState.toHtml() != (note?.content ?: "") || 
            color != (note?.color ?: 0xFFFFFFFF.toInt())
        }
    }

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
                richTextState = richTextState,
                onUndo = { undo() },
                onRedo = { redo() },
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    )
{ innerPadding ->
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
                            shape = CircleShape,
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

@Composable
fun FloatingToolbar(
    modifier: Modifier = Modifier,
    richTextState: RichTextState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
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
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "List",
                isActive = false 
            ) { richTextState.toggleUnorderedList() }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                isActive = false,
                enabled = canUndo
            ) { onUndo() }

            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                isActive = false,
                enabled = canRedo
            ) { onRedo() }
            
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                isActive -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
    }
}
