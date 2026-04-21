package com.example.agenttest.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.agenttest.data.local.entity.NoteType
import com.example.agenttest.ui.components.ChecklistViewer
import com.example.agenttest.util.SmartContextUtils
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.example.agenttest.data.local.entity.NoteEntity
import com.example.agenttest.ui.components.ImageViewerDialog
import com.example.agenttest.ui.theme.NoteShapes
import com.example.agenttest.ui.viewmodel.NoteViewModel
import com.example.agenttest.util.ColorUtils
import com.example.agenttest.util.DateUtils
import com.example.agenttest.util.NoteMetadataUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewScreen(
    noteId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val noteInList = remember(notes, noteId) { notes.find { it.id == noteId } }
    var lastKnownNote by remember { mutableStateOf<NoteEntity?>(null) }
    
    LaunchedEffect(noteInList) {
        if (noteInList != null) {
            lastKnownNote = noteInList
        }
    }
    
    val note = noteInList ?: lastKnownNote

    // Auto-navigate back if note is deleted and we're not already navigating
    var isNavigatingBack by remember { mutableStateOf(false) }
    LaunchedEffect(noteInList, isLoading) {
        if (noteInList == null && !isLoading && lastKnownNote != null && !isNavigatingBack) {
            isNavigatingBack = true
            onBackClick()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backgroundColor = if (note?.color != null && note.color != 0) Color(note.color) else MaterialTheme.colorScheme.surface
    val contentColor = if (note?.color != null && note.color != 0) ColorUtils.getContrastingColor(backgroundColor) else MaterialTheme.colorScheme.onSurface
    
    // Add this to handle the UI while note is missing (during deletion/restore transition)
    // We remove the early return that causes the blank surface
    /*
    if (noteInList == null && lastKnownNote != null && !isNavigatingBack) {
        Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(color = contentColor)
            }
        }
        return
    }
    */
    val secondaryContentColor = if (note?.color != null && note.color != 0) ColorUtils.getSecondaryContrastingColor(backgroundColor) else MaterialTheme.colorScheme.outline
    
    val titleStyle = NoteShapes.getTextStyleForColor(backgroundColor, MaterialTheme.typography.headlineLarge)
    val bodyStyle = NoteShapes.getTextStyleForColor(backgroundColor, MaterialTheme.typography.bodyLarge)

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val highlightedContent = remember(note?.content, searchQuery) {
        if (note == null || searchQuery.isEmpty()) null
        else NoteMetadataUtils.getHighlightedText(
            NoteMetadataUtils.stripHtml(note.content),
            searchQuery,
            Color.Yellow.copy(alpha = 0.5f)
        )
    }

    val highlightedTitle = remember(note?.title, searchQuery) {
        if (note == null || searchQuery.isEmpty()) null
        else NoteMetadataUtils.getHighlightedText(
            note.title,
            searchQuery,
            Color.Yellow.copy(alpha = 0.5f)
        )
    }
    
                val wordCount = note?.let { NoteMetadataUtils.getWordCount(it.content) } ?: 0
                val readingTime = note?.let { NoteMetadataUtils.getReadingTimeMinutes(it.content) } ?: 0
                
                val attachments = remember(note?.content) {
                    val html = note?.content ?: ""
                    val imageRegex = """image:([^"'\s>]+)""".toRegex()
                    val audioRegex = """audio:([^"'\s>]+)""".toRegex()
                    
                    val images = imageRegex.findAll(html).map { it.groupValues[1] to "image" }.toList()
                    val audios = audioRegex.findAll(html).map { it.groupValues[1] to "audio" }.toList()
                    
                    images + audios
                }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        if (note == null && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = backgroundColor,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search in note...", color = contentColor.copy(alpha = 0.6f)) },
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        cursorColor = contentColor,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = contentColor,
                                        unfocusedTextColor = contentColor
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = contentColor)
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                if (isSearchActive) {
                                    isSearchActive = false
                                    searchQuery = ""
                                } else if (!isNavigatingBack) {
                                    isNavigatingBack = true
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = contentColor
                                )
                            }
                        },
                        actions = {
                            if (note != null && !isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = contentColor
                                    )
                                }
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
                                IconButton(onClick = { 
                                    if (note.isDeleted) {
                                        showDeleteConfirm = true
                                    } else if (!isNavigatingBack) {
                                        isNavigatingBack = true
                                        viewModel.softDeleteNote(note.id)
                                        onBackClick()
                                    }
                                }) {
                                    Icon(
                                        if (note.isDeleted) Icons.Default.DeleteForever else Icons.Default.Delete,
                                        contentDescription = if (note.isDeleted) "Delete Permanently" else "Delete",
                                        tint = contentColor
                                    )
                                }
                                
                                if (note.isDeleted) {
                                    IconButton(onClick = {
                                        if (!isNavigatingBack) {
                                            isNavigatingBack = true
                                            viewModel.restoreNote(note.id)
                                            onBackClick()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = contentColor
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor,
                            titleContentColor = contentColor,
                            navigationIconContentColor = contentColor,
                            actionIconContentColor = contentColor
                        )
                    )
                }
            ) { innerPadding ->
                if (note != null) {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        if (highlightedTitle != null) {
                            Text(
                                text = highlightedTitle,
                                style = titleStyle.copy(fontWeight = FontWeight.Black),
                                color = contentColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = note.title,
                                style = titleStyle.copy(fontWeight = FontWeight.Black),
                                color = contentColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
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
                                
                                if (note.reminderTime != null && note.reminderTime > System.currentTimeMillis()) {
                                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.reminderTime))
                                    MetadataItem(Icons.Default.NotificationsActive, date, secondaryContentColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Last edited ${DateUtils.formatTimestamp(note.createdAt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryContentColor
                        )

                        if (note.labels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                note.labels.forEach { label ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(label) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = contentColor,
                                            containerColor = contentColor.copy(alpha = 0.1f)
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val audioPlayer = remember { com.example.agenttest.util.AudioPlayer(context) }
                        var showImageViewer by remember { mutableStateOf<Boolean>(false) }
                        var selectedImagePath by remember { mutableStateOf<String>("") }
                        
                        DisposableEffect(Unit) {
                            onDispose { audioPlayer.stopAudio() }
                        }

                        if (note.type == NoteType.TEXT) {
                            val richTextState = rememberRichTextState()

                            LaunchedEffect(note.content) {
                                richTextState.setHtml(note.content)
                            }
                            
                            LaunchedEffect(richTextState.selection) {
                                if (richTextState.isLink) {
                                    val url = richTextState.selectedLinkUrl
                                    if (url?.startsWith("checkbox:") == true) {
                                        val isChecked = url == "checkbox:checked"
                                        val newUrl = if (isChecked) "checkbox:unchecked" else "checkbox:checked"
                                        val newText = if (isChecked) "☐ " else "☑ "
                                        
                                        val annotations = richTextState.annotatedString.getStringAnnotations("URL", richTextState.selection.start, richTextState.selection.end)
                                        val annotation = annotations.firstOrNull { it.item.startsWith("checkbox:") }
                                        
                                        if (annotation != null) {
                                            val oldContent = note.content
                                            richTextState.selection = androidx.compose.ui.text.TextRange(annotation.start, annotation.end)
                                            richTextState.addLink(newText, newUrl)
                                            richTextState.selection = androidx.compose.ui.text.TextRange(annotation.start + newText.length)
                                            
                                            val newContent = richTextState.toHtml()
                                            
                                            // Save the updated note content
                                            viewModel.saveNote(
                                                id = note.id,
                                                title = note.title,
                                                content = newContent,
                                                color = note.color
                                            )

                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                val result = snackbarHostState.showSnackbar(
                                                    message = if (isChecked) "Checkbox unchecked" else "Checkbox checked",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.saveNote(
                                                        id = note.id,
                                                        title = note.title,
                                                        content = oldContent,
                                                        color = note.color
                                                    )
                                                }
                                            }
                                        }
                                    } else if (url?.startsWith("audio:") == true) {
                                        val path = url.removePrefix("audio:")
                                        try {
                                            audioPlayer.playAudio(path)
                                            android.widget.Toast.makeText(context, "Playing voice memo...", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Playback error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else if (url?.startsWith("image:") == true) {
                                         val path = url.removePrefix("image:")
                                         selectedImagePath = path
                                         showImageViewer = true
                                    }
                                }
                            }

                            if (showImageViewer) {
                                ImageViewerDialog(
                                    imagePath = selectedImagePath,
                                    onDismiss = { showImageViewer = false }
                                )
                            }

                            if (isSearchActive && searchQuery.isNotEmpty() && highlightedContent != null) {
                                Text(
                                    text = highlightedContent,
                                    style = bodyStyle.copy(lineHeight = bodyStyle.lineHeight * 1.2),
                                    color = contentColor
                                )
                            } else {
                                RichText(
                                    state = richTextState,
                                    style = bodyStyle.copy(lineHeight = bodyStyle.lineHeight * 1.2),
                                    color = contentColor
                                )
                            }
                        } else {
                            ChecklistViewer(
                                items = note.checklistItems,
                                contentColor = contentColor,
                                searchQuery = searchQuery,
                                onToggleItem = { toggledItem ->
                                    val oldItems = note.checklistItems.toList()
                                    val updatedItems = oldItems.map { 
                                        if (it.id == toggledItem.id) it.copy(isChecked = !it.isChecked) else it 
                                    }
                                    viewModel.saveNote(
                                        id = note.id,
                                        title = note.title,
                                        content = note.content,
                                        type = note.type,
                                        checklistItems = updatedItems,
                                        color = note.color
                                    )

                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (!toggledItem.isChecked) "Item checked" else "Item unchecked",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.saveNote(
                                                id = note.id,
                                                title = note.title,
                                                content = note.content,
                                                type = note.type,
                                                checklistItems = oldItems,
                                                color = note.color
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        if (showImageViewer) {
                            ImageViewerDialog(
                                imagePath = selectedImagePath,
                                onDismiss = { showImageViewer = false }
                            )
                        }

                        if (attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "ATTACHMENTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachments.forEach { (path, type) ->
                        val isHighlighted = searchQuery.isNotEmpty() && (
                            (type == "audio" && "Voice Memo".contains(searchQuery, ignoreCase = true)) ||
                            (type == "image" && "Sketch".contains(searchQuery, ignoreCase = true))
                        )
                        AssistChip(
                            onClick = {
                                if (type == "audio") {
                                    try {
                                        audioPlayer.playAudio(path)
                                        android.widget.Toast.makeText(context, "Playing recording...", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Playback error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedImagePath = path
                                    showImageViewer = true
                                }
                            },
                            label = { 
                                val labelText = if (type == "audio") "Voice Memo" else "Sketch"
                                if (isHighlighted) {
                                    Text(
                                        text = NoteMetadataUtils.getHighlightedText(
                                            labelText,
                                            searchQuery,
                                            Color.Yellow.copy(alpha = 0.5f)
                                        )
                                    )
                                } else {
                                    Text(labelText)
                                }
                            },
                                leadingIcon = {
                                    Icon(
                                        if (type == "audio") Icons.Outlined.Mic else Icons.Outlined.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = contentColor,
                                    leadingIconContentColor = contentColor,
                                    containerColor = contentColor.copy(alpha = 0.1f)
                                ),
                                border = null
                            )
                        }
                    }
                }

                        val contextActions = remember(note.content) {
                            com.example.agenttest.util.SmartContextUtils.extractActions(NoteMetadataUtils.stripHtml(note.content))
                        }

                        if (contextActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Smart Actions",
                                style = MaterialTheme.typography.labelLarge,
                                color = secondaryContentColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                contextActions.forEach { action ->
                                    val isHighlighted = searchQuery.isNotEmpty() && action.label.contains(searchQuery, ignoreCase = true)
                                    AssistChip(
                                        onClick = {
                                            try {
                                                val intent = when (action.type) {
                                                    SmartContextUtils.ActionType.URL -> Intent(Intent.ACTION_VIEW, Uri.parse(action.data))
                                                    SmartContextUtils.ActionType.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${action.data}"))
                                                    SmartContextUtils.ActionType.PHONE -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.data}"))
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        label = { 
                                            if (isHighlighted) {
                                                Text(
                                                    text = NoteMetadataUtils.getHighlightedText(
                                                        action.label,
                                                        searchQuery,
                                                        Color.Yellow.copy(alpha = 0.5f)
                                                    )
                                                )
                                            } else {
                                                Text(action.label)
                                            }
                                        },
                                        leadingIcon = { Icon(action.icon, null, modifier = Modifier.size(18.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            labelColor = contentColor,
                                            leadingIconContentColor = contentColor
                                        )
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently removed.") },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        if (!isNavigatingBack) {
                            isNavigatingBack = true
                            if (note.isDeleted) {
                                viewModel.deleteNotePermanently(note)
                            } else {
                                viewModel.softDeleteNote(note.id)
                            }
                            onBackClick()
                        }
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
