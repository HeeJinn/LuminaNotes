package com.example.agenttest.ui.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Mic
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.agenttest.MainActivity
import com.example.agenttest.ui.components.DrawingDialog
import com.example.agenttest.ui.components.ImageViewerDialog
import com.example.agenttest.ui.components.RecordingDialog
import com.example.agenttest.ui.home.DarkNoteColors
import com.example.agenttest.ui.home.NoteColors
import com.example.agenttest.ui.theme.NoteShapes
import com.example.agenttest.ui.viewmodel.NoteViewModel
import com.example.agenttest.util.AudioPlayer
import com.example.agenttest.util.ColorUtils
import com.example.agenttest.util.NoteMetadataUtils
import com.example.agenttest.data.local.entity.ChecklistItem
import com.example.agenttest.data.local.entity.NoteEntity
import com.example.agenttest.data.local.entity.NoteType
import com.example.agenttest.ui.components.ChecklistEditor
import com.example.agenttest.ui.components.ChecklistViewer
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream

// History Snapshot
data class NoteSnapshot(
    val title: String,
    val content: String,
    val type: NoteType,
    val checklistItems: List<ChecklistItem>,
    val labels: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String? = null,
    onBackClick: () -> Unit,
    onDeleteFinished: () -> Unit,
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val note = notes.find { it.id == noteId }
    
    var isInitialized by remember { mutableStateOf(false) }

    // State variables
    var title by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf(NoteType.TEXT) }
    var noteColor by remember { mutableIntStateOf(0) }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }
    val noteLabels = remember { mutableStateListOf<String>() }
    var isLocked by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableLongStateOf(0L) }
    val richTextState = rememberRichTextState()
    
    val focusRequester = remember { FocusRequester() }
    var isNavigatingBack by remember { mutableStateOf(false) }
    
    // History State
    val undoStack = remember { mutableStateListOf<NoteSnapshot>() }
    val redoStack = remember { mutableStateListOf<NoteSnapshot>() }
    var isInternalUpdate by remember { mutableStateOf(false) }
    var lastStateForHistory by remember { 
        mutableStateOf<NoteSnapshot?>(null)
    }

    // Initialize state when note loads
    LaunchedEffect(note) {
        if (note != null && !isInitialized) {
            isInternalUpdate = true
            title = note.title
            noteType = note.type
            noteColor = note.color
            checklistItems.clear()
            checklistItems.addAll(note.checklistItems)
            noteLabels.clear()
            noteLabels.addAll(note.labels)
            isLocked = note.isLocked
            isPinned = note.isPinned
            reminderTime = note.reminderTime ?: 0L
            
            if (richTextState.annotatedString.text.isEmpty()) {
                richTextState.setHtml(note.content)
            }
            
            val snapshot = NoteSnapshot(
                note.title,
                note.content,
                note.type,
                note.checklistItems,
                note.labels
            )
            lastStateForHistory = snapshot
            
            isInitialized = true
            isInternalUpdate = false
        } else if (note == null && noteId == null && !isInitialized) {
            // New note initialization
            isInitialized = true
            lastStateForHistory = NoteSnapshot("", "", NoteType.TEXT, emptyList(), emptyList())
        }
    }

    val isDark = isSystemInDarkTheme()
    val targetBackgroundColor = if (noteColor == 0) MaterialTheme.colorScheme.surface else Color(noteColor)
    val bodyStyle = NoteShapes.getTextStyleForColor(targetBackgroundColor, MaterialTheme.typography.bodyLarge)

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val highlightedTitle = remember(title, searchQuery) {
        if (searchQuery.isEmpty()) null
        else NoteMetadataUtils.getHighlightedText(
            title,
            searchQuery,
            Color.Yellow.copy(alpha = 0.5f)
        )
    }

    val highlightedContent = remember(richTextState.annotatedString.text, searchQuery) {
        if (searchQuery.isEmpty()) null
        else NoteMetadataUtils.getHighlightedText(
            richTextState.annotatedString.text,
            searchQuery,
            Color.Yellow.copy(alpha = 0.5f)
        )
    }
    val targetContentColor = ColorUtils.getContrastingColor(targetBackgroundColor)
    val targetSecondaryColor = ColorUtils.getSecondaryContrastingColor(targetBackgroundColor)

    val animatedColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 300),
        label = "contentColor"
    )
    
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 300),
        label = "secondaryColor"
    )

    // Remove the aggressive navigation trigger that causes white screens
    /*
    LaunchedEffect(note, noteId, isLoading) {
        if (note == null && noteId != null && !isLoading && isInitialized) {
            onDeleteFinished()
        }
    }
    */

    if (noteId != null && !isInitialized) {
        // Show a loading screen with the target background if we know it, or surface
        val loadingBg = if (noteColor != 0) Color(noteColor) else MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(loadingBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorUtils.getContrastingColor(loadingBg))
        }
        return
    }

    /*
    if (noteId != null && note == null) {
        // Show the same background color while exiting to prevent white screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedColor),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = contentColor)
            }
        }
        return
    }
    */

    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayer(context) }
    
    fun findActivity(context: Context): MainActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is MainActivity) return currentContext
            currentContext = (currentContext as ContextWrapper).baseContext
        }
        return null
    }

    val activity = remember(context) { findActivity(context) }

    var isAuthenticated by remember { mutableStateOf(false) }

    fun requestAuthentication(onSuccess: () -> Unit) {
        activity?.showBiometricPrompt(
            title = "Vault Access",
            subtitle = "Authenticate to view this note",
            onSuccess = {
                isAuthenticated = true
                onSuccess()
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                onBackClick() // If authentication fails, we should not see the note
            }
        )
    }


    LaunchedEffect(note, noteId) {
        if (note?.isLocked == true && !isAuthenticated) {
            requestAuthentication(
                onSuccess = { /* User authenticated */ }
            ) ?: run {
                // If activity is null, we can't show the prompt
                onBackClick()
            }
        }
    }

    if (note?.isLocked == true && !isAuthenticated) {
        // Show an empty screen or loading indicator while authenticating
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Note is locked", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { requestAuthentication {} }) {
                    Text("Unlock")
                }
            }
        }
        return
    }
    
    var showReminderPicker by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stopAudio()
        }
    }

    fun getCurrentSnapshot() = NoteSnapshot(
        title = title,
        content = richTextState.toHtml(),
        type = noteType,
        checklistItems = checklistItems.toList(),
        labels = noteLabels.toList()
    )

    fun captureStateForHistory() {
        if (isInternalUpdate) return
        val currentState = getCurrentSnapshot()
        if (lastStateForHistory != null && currentState != lastStateForHistory) {
            undoStack.add(lastStateForHistory!!)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            lastStateForHistory = currentState
        } else if (lastStateForHistory == null) {
            lastStateForHistory = currentState
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isInternalUpdate = true
            val currentState = getCurrentSnapshot()
            redoStack.add(currentState)
            val lastState = undoStack.removeAt(undoStack.size - 1)
            title = lastState.title
            richTextState.setHtml(lastState.content)
            noteType = lastState.type
            checklistItems.clear()
            checklistItems.addAll(lastState.checklistItems)
            noteLabels.clear()
            noteLabels.addAll(lastState.labels)
            lastStateForHistory = lastState
            isInternalUpdate = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isInternalUpdate = true
            val currentState = getCurrentSnapshot()
            undoStack.add(currentState)
            val nextState = redoStack.removeAt(redoStack.size - 1)
            title = nextState.title
            richTextState.setHtml(nextState.content)
            noteType = nextState.type
            checklistItems.clear()
            checklistItems.addAll(nextState.checklistItems)
            noteLabels.clear()
            noteLabels.addAll(nextState.labels)
            lastStateForHistory = nextState
            isInternalUpdate = false
        }
    }

    // Observe changes for history
    LaunchedEffect(title, richTextState.annotatedString, noteType, checklistItems.toList(), noteLabels.toList()) {
        if (!isInternalUpdate) {
            delay(500)
            captureStateForHistory()
        }
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
                    richTextState.selection = androidx.compose.ui.text.TextRange(annotation.start, annotation.end)
                    richTextState.addLink(newText, newUrl)
                    // Move selection to end of the checkbox to avoid immediate re-trigger
                    richTextState.selection = androidx.compose.ui.text.TextRange(annotation.start + newText.length)
                }
            } else if (url?.startsWith("audio:") == true) {
                val path = url.removePrefix("audio:")
                try {
                    audioPlayer.playAudio(path)
                    Toast.makeText(context, "Playing voice memo...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                // Clear selection to avoid double-triggering or staying in "link mode"
                richTextState.selection = androidx.compose.ui.text.TextRange(richTextState.selection.max)
            } else if (url?.startsWith("image:") == true) {
                val path = url.removePrefix("image:")
                selectedImagePath = path
                showImageViewer = true
                richTextState.selection = androidx.compose.ui.text.TextRange(richTextState.selection.max)
            }
        }
    }

    fun showDateTimePicker() {
        val calendar = java.util.Calendar.getInstance()
        if (reminderTime > 0) calendar.timeInMillis = reminderTime

        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = java.util.Calendar.getInstance()
                        picked.set(year, month, day, hour, minute, 0)
                        reminderTime = picked.timeInMillis
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    // Extract attachments from HTML for the media bar
    val attachments = remember(richTextState.toHtml()) {
        val html = richTextState.toHtml()
        val imageRegex = """image:([^"'\s>]+)""".toRegex()
        val audioRegex = """audio:([^"'\s>]+)""".toRegex()
        
        val images = imageRegex.findAll(html).map { it.groupValues[1] to "image" }.toList()
        val audios = audioRegex.findAll(html).map { it.groupValues[1] to "audio" }.toList()
        
        images + audios
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSoftDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelManager by remember { mutableStateOf(false) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var showRecordingDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showRecordingDialog = true
        } else {
            Toast.makeText(context, "Permission denied for recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestAudioPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                showRecordingDialog = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun insertDrawing(bitmap: Bitmap) {
        try {
            val file = java.io.File(context.filesDir, "drawing_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Insert a non-empty link so the editor doesn't strip it
            val imgHtml = """<a href="image:${file.absolutePath}">🎨</a>"""
            richTextState.insertHtml(imgHtml, richTextState.selection.end)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save drawing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        showDrawingCanvas = false
    }

    fun insertVoiceMemo(path: String) {
        // Insert a non-empty link so the editor doesn't strip it
        val voiceHtml = """<a href="audio:$path">🎙️</a>"""
        richTextState.insertHtml(voiceHtml, richTextState.selection.end)
        showRecordingDialog = false
    }

    val hasChanges by remember {
        derivedStateOf {
            title != (note?.title ?: "") || 
            richTextState.toHtml() != (note?.content ?: "") || 
            noteColor != (note?.color ?: 0) ||
            noteType != (note?.type ?: NoteType.TEXT) ||
            checklistItems.toList() != (note?.checklistItems ?: emptyList<ChecklistItem>()) ||
            noteLabels.toList() != (note?.labels ?: emptyList<String>()) ||
            isLocked != (note?.isLocked ?: false) ||
            reminderTime != (note?.reminderTime ?: 0L)
        }
    }

    // Auto-suggest color based on content
    LaunchedEffect(title, richTextState.annotatedString.text) {
        if (noteId == null && noteColor == 0) {
            NoteMetadataUtils.suggestColor(title, richTextState.annotatedString.text)?.let { suggested ->
                noteColor = suggested
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

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp, color = animatedColor) {
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
                                handleBack()
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
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = contentColor
                                )
                            }
                            
                            IconButton(onClick = { 
                                isPinned = !isPinned
                            }) {
                                Icon(
                                    if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else contentColor
                                )
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = contentColor)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Reminder") },
                                        onClick = { showMenu = false; showDateTimePicker() },
                                        leadingIcon = {
                                            Icon(
                                                if (reminderTime > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Change color") },
                                        onClick = { showMenu = false; showColorPicker = true },
                                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isLocked) "Unlock note" else "Lock note") },
                                        onClick = { 
                                            showMenu = false
                                            if (isLocked) {
                                                requestAuthentication { isLocked = false }
                                            } else {
                                                isLocked = true
                                            }
                                        },
                                        leadingIcon = { Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null) }
                                    )
                                    
                                    if (note != null) {
                                        HorizontalDivider()
                                        if (note.isDeleted) {
                                            DropdownMenuItem(
                                                text = { Text("Restore") },
                                                onClick = {
                                                    showMenu = false
                                                    if (!isNavigatingBack) {
                                                        isNavigatingBack = true
                                                        viewModel.restoreNote(note.id)
                                                        onDeleteFinished()
                                                    }
                                                },
                                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Permanently") },
                                                onClick = { showMenu = false; showDeleteDialog = true },
                                                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = { showMenu = false; showSoftDeleteDialog = true },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    if (title.isNotBlank() || richTextState.annotatedString.text.isNotBlank() || checklistItems.isNotEmpty()) {
                                        val htmlContent = richTextState.toHtml()
                                        viewModel.saveNote(
                                            noteId,
                                            title,
                                            htmlContent,
                                            type = noteType,
                                            checklistItems = checklistItems.toList(),
                                            color = noteColor,
                                            reminderTime = if (reminderTime > 0) reminderTime else null,
                                            labels = noteLabels.toList(),
                                            isLocked = isLocked,
                                            isPinned = isPinned
                                        )
                                    }
                                    onBackClick()
                                },
                                enabled = note?.isDeleted != true,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor,
                                    contentColor = animatedColor
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = animatedColor,
        bottomBar = {
            FloatingToolbar(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                richTextState = richTextState,
                noteType = noteType,
                checklistItems = checklistItems,
                onNoteTypeChanged = { noteType = it },
                onUndo = { undo() },
                onRedo = { redo() },
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onDrawClick = { showDrawingCanvas = true },
                onMicClick = { checkAndRequestAudioPermission() }
            )
        }
    ) { innerPadding ->
        if (showDrawingCanvas) {
            DrawingDialog(
                onDismiss = { showDrawingCanvas = false },
                onSave = { bitmap -> insertDrawing(bitmap) }
            )
        }
        if (showRecordingDialog) {
            RecordingDialog(
                onDismiss = { showRecordingDialog = false },
                onSave = { path -> insertVoiceMemo(path) }
            )
        }
        if (showImageViewer) {
            ImageViewerDialog(
                imagePath = selectedImagePath,
                onDismiss = { showImageViewer = false }
            )
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Metadata Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val words = NoteMetadataUtils.getWordCount(richTextState.toHtml())
                val readTime = NoteMetadataUtils.getReadingTimeMinutes(richTextState.toHtml())
                
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = secondaryColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$readTime min read",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
                
                Spacer(Modifier.width(16.dp))
                
                Icon(Icons.Default.StickyNote2, null, modifier = Modifier.size(14.dp), tint = secondaryColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$words words",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
                
                if (note != null) {
                    Spacer(Modifier.weight(1f))
                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.createdAt))
                    Text(
                        text = "Edited $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (noteLabels.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    noteLabels.forEach { label ->
                        InputChip(
                            selected = true,
                            onClick = { /* Remove label */ noteLabels.remove(label) },
                            label = { Text(label) },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = contentColor.copy(alpha = 0.1f),
                                selectedLabelColor = contentColor,
                                selectedTrailingIconColor = contentColor
                            ),
                            border = null
                        )
                    }
                    
                    AssistChip(
                        onClick = { showLabelManager = true },
                        label = { Text("Add Label") },
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = contentColor.copy(alpha = 0.6f),
                            leadingIconContentColor = contentColor.copy(alpha = 0.6f)
                        ),
                        border = null
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                TextButton(
                    onClick = { showLabelManager = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(18.dp), tint = secondaryColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Add labels", color = secondaryColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (highlightedTitle != null) {
                Text(
                    text = highlightedTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { isSearchActive = false }
                )
            } else {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note Title", style = MaterialTheme.typography.displaySmall, color = contentColor.copy(alpha = 0.3f)) },
                    textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = contentColor),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = contentColor,
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (noteType == NoteType.TEXT) {
                if (isSearchActive && searchQuery.isNotEmpty() && highlightedContent != null) {
                    Text(
                        text = highlightedContent,
                        style = bodyStyle.copy(color = contentColor),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { isSearchActive = false }
                    )
                } else {
                    CompositionLocalProvider(
                        LocalContentColor provides contentColor
                    ) {
                        RichTextEditor(
                            state = richTextState,
                            placeholder = { Text("Write something amazing...", style = bodyStyle, color = contentColor.copy(alpha = 0.3f)) },
                            textStyle = bodyStyle.copy(color = contentColor),
                            colors = RichTextEditorDefaults.richTextEditorColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = contentColor,
                                textColor = contentColor
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
                        )
                    }
                }
            } else {
                if (isSearchActive && searchQuery.isNotEmpty()) {
                    ChecklistViewer(
                        items = checklistItems,
                        contentColor = contentColor,
                        searchQuery = searchQuery,
                        onToggleItem = { toggledItem ->
                            val index = checklistItems.indexOfFirst { it.id == toggledItem.id }
                            if (index != -1) {
                                checklistItems[index] = toggledItem.copy(isChecked = !toggledItem.isChecked)
                            }
                        }
                    )
                } else {
                    ChecklistEditor(
                        items = checklistItems,
                        contentColor = contentColor,
                        onItemsChanged = { /* Handled by mutableStateListOf */ }
                    )
                }
            }

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "ATTACHMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                                        Toast.makeText(context, "Playing recording...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
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
            
            // Add spacing at the bottom so content isn't hidden by the floating toolbar
            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showColorPicker) {
        ModalBottomSheet(
            onDismissRequest = { showColorPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Select color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                val colors = (if (isDark) DarkNoteColors else NoteColors).drop(1)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Surface(
                            onClick = { 
                                noteColor = 0 // Use 0 for "Follow Theme"
                                showColorPicker = false 
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FormatColorReset, 
                                    contentDescription = "Default",
                                    modifier = Modifier.size(24.dp)
                                )
                                if (noteColor == 0 || noteColor == 0xFFFFFFFF.toInt() || noteColor == 0xFF1F1F1F.toInt()) {
                                    Icon(
                                        Icons.Default.Check, 
                                        null, 
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    items(colors.size) { index ->
                        val itemColor = colors[index]
                        Surface(
                            onClick = { noteColor = itemColor.toArgb(); showColorPicker = false },
                            shape = CircleShape,
                            color = itemColor,
                            modifier = Modifier.size(48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            if (noteColor == itemColor.toArgb()) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently removed.") },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        if (!isNavigatingBack) {
                            isNavigatingBack = true
                            viewModel.deleteNotePermanently(note)
                            onDeleteFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showSoftDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showSoftDeleteDialog = false },
            title = { Text("Move to Trash?") },
            text = { Text("You can restore this note from the Trash later.") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            confirmButton = {
                Button(
                    onClick = {
                        showSoftDeleteDialog = false
                        if (!isNavigatingBack) {
                            isNavigatingBack = true
                            viewModel.softDeleteNote(note.id)
                            onDeleteFinished()
                        }
                    }
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoftDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showLabelManager) {
        val allLabels by viewModel.allLabels.collectAsState()
        var labelQuery by remember { mutableStateOf("") }
        
        ModalBottomSheet(
            onDismissRequest = { showLabelManager = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Labels", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = labelQuery,
                    onValueChange = { labelQuery = it },
                    placeholder = { Text("Enter label name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (labelQuery.isNotBlank()) {
                            IconButton(onClick = { 
                                if (!noteLabels.contains(labelQuery)) {
                                    noteLabels.add(labelQuery)
                                }
                                labelQuery = ""
                            }) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                val filteredLabels = allLabels.filter { it.contains(labelQuery, ignoreCase = true) }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredLabels.size) { index ->
                        val label = filteredLabels[index]
                        FilterChip(
                            selected = noteLabels.contains(label),
                            onClick = {
                                if (noteLabels.contains(label)) noteLabels.remove(label)
                                else noteLabels.add(label)
                            },
                            label = { Text(label) }
                        )
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
    noteType: NoteType,
    checklistItems: MutableList<ChecklistItem>,
    onNoteTypeChanged: (NoteType) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onDrawClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val h1Size = MaterialTheme.typography.headlineLarge.fontSize
    val h2Size = MaterialTheme.typography.titleLarge.fontSize

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (noteType == NoteType.TEXT) {
                ToolbarButton(
                    icon = Icons.Default.Title,
                    contentDescription = "Big Font",
                    isActive = false
                ) { richTextState.toggleSpanStyle(SpanStyle(fontSize = h1Size)) }
                
                ToolbarButton(
                    icon = Icons.Default.FormatSize,
                    contentDescription = "Small Font",
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
            }

            ToolbarButton(
                icon = if (noteType == NoteType.CHECKLIST) Icons.AutoMirrored.Filled.Notes else Icons.Default.CheckBox,
                contentDescription = "Toggle Checklist Mode",
                isActive = noteType == NoteType.CHECKLIST
            ) { 
                if (noteType == NoteType.TEXT) {
                    // Convert TEXT to CHECKLIST
                    val text = richTextState.annotatedString.text
                    if (text.isNotBlank()) {
                        val lines = text.split("\n").filter { it.isNotBlank() }
                        checklistItems.clear()
                        lines.forEach { checklistItems.add(ChecklistItem(text = it)) }
                    } else if (checklistItems.isEmpty()) {
                        checklistItems.add(ChecklistItem(text = ""))
                    }
                    onNoteTypeChanged(NoteType.CHECKLIST)
                } else {
                    // Convert CHECKLIST to TEXT
                    val text = checklistItems.joinToString("\n") { it.text }
                    richTextState.setHtml("<p>$text</p>")
                    onNoteTypeChanged(NoteType.TEXT)
                }
            }

            if (noteType == NoteType.TEXT) {
                ToolbarButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullets",
                    isActive = false 
                ) { richTextState.toggleUnorderedList() }

                ToolbarButton(
                    icon = Icons.Default.FormatListNumbered,
                    contentDescription = "Numbers",
                    isActive = false 
                ) { richTextState.toggleOrderedList() }

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

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
            
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ToolbarButton(
                icon = Icons.Default.Brush,
                contentDescription = "Draw",
                isActive = false
            ) { onDrawClick() }

            ToolbarButton(
                icon = Icons.Default.Mic,
                contentDescription = "Voice Memo",
                isActive = false
            ) { onMicClick() }
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
