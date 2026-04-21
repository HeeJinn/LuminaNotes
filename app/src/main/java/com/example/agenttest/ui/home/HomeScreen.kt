package com.example.agenttest.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.agenttest.R
import com.example.agenttest.data.local.entity.NoteEntity
import com.example.agenttest.ui.theme.NoteShapes
import com.example.agenttest.ui.viewmodel.NoteFilter
import com.example.agenttest.ui.viewmodel.NoteViewModel
import com.example.agenttest.data.local.entity.NoteType
import com.example.agenttest.ui.components.ChecklistViewer
import com.example.agenttest.util.ColorUtils
import com.example.agenttest.util.DateUtils
import com.example.agenttest.util.NoteMetadataUtils
import com.example.agenttest.util.SmartContextUtils
import kotlinx.coroutines.launch

val NoteColors = listOf(
    Color.White,
    Color(0xFFF28B82), // Red
    Color(0xFFFBBC04), // Orange
    Color(0xFFFFF475), // Yellow
    Color(0xFFCCFF90), // Green
    Color(0xFFA7FFEB), // Teal
    Color(0xFFCBF0F8), // Blue
    Color(0xFFAECBFA), // Dark Blue
    Color(0xFFD7AEFB), // Purple
    Color(0xFFFDCFE8), // Pink
    Color(0xFFE6C9A8), // Brown
    Color(0xFFE8EAED)  // Gray
)

val DarkNoteColors = listOf(
    Color(0xFF1F1F1F),
    Color(0xFF5C2B29),
    Color(0xFF5F4401),
    Color(0xFF635D19),
    Color(0xFF345920),
    Color(0xFF16504B),
    Color(0xFF2D555E),
    Color(0xFF1E3A5F),
    Color(0xFF42275E),
    Color(0xFF5B2245),
    Color(0xFF442F19),
    Color(0xFF3C4043)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNoteClick: (String) -> Unit,
    onAddNoteClick: () -> Unit,
    viewModel: NoteViewModel
) {
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val allLabels by viewModel.allLabels.collectAsState()
    val selectedLabel by viewModel.selectedLabel.collectAsState()
    var active by remember { mutableStateOf(false) }
    
    var showOptionsForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showColorPickerForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDeletePermanently by remember { mutableStateOf<NoteEntity?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = when(currentFilter) {
                            NoteFilter.ALL -> "My Notes"
                            NoteFilter.PINNED -> "Pinned"
                            NoteFilter.ARCHIVED -> "Archived"
                            NoteFilter.DELETED -> "Trash"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .padding(top = 8.dp)
                    )
                    
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChange(it) },
                                onSearch = { active = false },
                                expanded = active,
                                onExpandedChange = { active = it },
                                placeholder = { Text("Search your notes", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    if (active || searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { 
                                            if (searchQuery.isNotEmpty()) viewModel.onSearchQueryChange("") 
                                            else active = false 
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = active,
                        onExpandedChange = { active = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (active) 0.dp else 16.dp)
                            .padding(bottom = if (active) 0.dp else 16.dp),
                        content = {
                            // In Search Results, we should show highlighted text for attachments too
                            val searchResults by remember(notes, searchQuery) {
                                derivedStateOf {
                                    if (searchQuery.isEmpty()) emptyList<NoteEntity>()
                                    else notes.filter { note ->
                                        note.title.contains(searchQuery, ignoreCase = true) || 
                                        com.example.agenttest.util.NoteMetadataUtils.stripHtml(note.content).contains(searchQuery, ignoreCase = true)
                                    }
                                }
                            }
                            
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalItemSpacing = 8.dp,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults, key = { it.id }) { note ->
                                    NoteSearchResultItem(note, onNoteClick)
                                }
                            }
                        }
                    )

                    if (!active) {
                        FilterChips(
                            currentFilter = currentFilter,
                            allLabels = allLabels,
                            selectedLabel = selectedLabel,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onLabelSelected = { viewModel.setSelectedLabel(it) }
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddNoteClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Note", style = MaterialTheme.typography.labelLarge) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                } else if (notes.isEmpty()) {
                    EmptyState(searchQuery, currentFilter)
                } else {
                    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
                    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }
                    val showSections = currentFilter == NoteFilter.ALL && pinnedNotes.isNotEmpty() && unpinnedNotes.isNotEmpty()

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showSections) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader("Pinned")
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    modifier = Modifier.animateItem(),
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onLongClick = { showOptionsForNote = note },
                                    onPinClick = { viewModel.togglePin(note) },
                                    searchQuery = searchQuery
                                )
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader("Others")
                            }
                            items(unpinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    modifier = Modifier.animateItem(),
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onLongClick = { showOptionsForNote = note },
                                    onPinClick = { viewModel.togglePin(note) },
                                    searchQuery = searchQuery
                                )
                            }
                        } else {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(
                                    modifier = Modifier.animateItem(),
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onLongClick = { showOptionsForNote = note },
                                    onPinClick = { viewModel.togglePin(note) },
                                    searchQuery = searchQuery
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showOptionsForNote != null) {
            NoteOptionsSheet(
                note = showOptionsForNote!!,
                onDismiss = { showOptionsForNote = null },
                onPinToggle = { viewModel.togglePin(it); showOptionsForNote = null },
                onArchiveToggle = { 
                    val note = it
                    viewModel.toggleArchive(note)
                    showOptionsForNote = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (note.isArchived) "Note unarchived" else "Note archived",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.toggleArchive(note)
                        }
                    }
                },
                onDelete = { 
                    viewModel.softDeleteNote(it.id)
                    showOptionsForNote = null 
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Note moved to Trash",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreNote(it.id)
                        }
                    }
                },
                onColorPicker = { showColorPickerForNote = it; showOptionsForNote = null },
                onRestore = { 
                    viewModel.restoreNote(it.id)
                    showOptionsForNote = null
                },
                onDeletePermanently = {
                    noteToDeletePermanently = it
                    showOptionsForNote = null
                },
                onEmptyTrash = {
                    showEmptyTrashDialog = true
                    showOptionsForNote = null
                }
            )
        }

        if (noteToDeletePermanently != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteNotePermanently(noteToDeletePermanently!!)
                    noteToDeletePermanently = null
                },
                onDismiss = { noteToDeletePermanently = null }
            )
        }

        if (showEmptyTrashDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                title = { Text("Empty Trash?") },
                text = { Text("All notes in the trash will be permanently deleted. This action cannot be undone.") },
                icon = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.emptyTrash()
                            showEmptyTrashDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Empty Trash")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showColorPickerForNote != null) {
            ColorPickerSheet(
                note = showColorPickerForNote!!,
                onDismiss = { showColorPickerForNote = null },
                onColorSelected = { note, color -> 
                    viewModel.updateNoteColor(note, color.toArgb())
                    showColorPickerForNote = null
                }
            )
        }

        if (noteToDelete != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    val note = noteToDelete!!
                    viewModel.softDeleteNote(note.id)
                    noteToDelete = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Note moved to Trash",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreNote(note.id)
                        }
                    }
                },
                onDismiss = { noteToDelete = null }
            )
        }
    }
}

@Composable
fun FilterChips(
    currentFilter: NoteFilter,
    allLabels: List<String>,
    selectedLabel: String?,
    onFilterSelected: (NoteFilter) -> Unit,
    onLabelSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == NoteFilter.ALL && selectedLabel == null,
            onClick = { onFilterSelected(NoteFilter.ALL) },
            label = { Text("All") },
            shape = CircleShape,
            leadingIcon = if (currentFilter == NoteFilter.ALL && selectedLabel == null) {
                { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null
        )
        
        allLabels.forEach { label ->
            FilterChip(
                selected = selectedLabel == label,
                onClick = { 
                    if (selectedLabel == label) onLabelSelected(null)
                    else onLabelSelected(label)
                },
                label = { Text(label) },
                shape = CircleShape,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            )
        }

        FilterChip(
            selected = currentFilter == NoteFilter.PINNED,
            onClick = { onFilterSelected(NoteFilter.PINNED) },
            label = { Text("Pinned") },
            shape = CircleShape,
            leadingIcon = if (currentFilter == NoteFilter.PINNED) {
                { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null
        )
        FilterChip(
            selected = currentFilter == NoteFilter.ARCHIVED,
            onClick = { onFilterSelected(NoteFilter.ARCHIVED) },
            label = { Text("Archived") },
            shape = CircleShape,
            leadingIcon = if (currentFilter == NoteFilter.ARCHIVED) {
                { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null
        )
        FilterChip(
            selected = currentFilter == NoteFilter.DELETED,
            onClick = { onFilterSelected(NoteFilter.DELETED) },
            label = { Text("Trash") },
            shape = CircleShape,
            leadingIcon = if (currentFilter == NoteFilter.DELETED) {
                { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteOptionsSheet(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onPinToggle: (NoteEntity) -> Unit,
    onArchiveToggle: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onColorPicker: (NoteEntity) -> Unit,
    onRestore: (NoteEntity) -> Unit,
    onDeletePermanently: (NoteEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            if (!note.isDeleted) {
                ListItem(
                    headlineContent = { Text(if (note.isPinned) "Unpin" else "Pin") },
                    leadingContent = { Icon(if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, null) },
                    modifier = Modifier.clickable { onPinToggle(note) }
                )
                ListItem(
                    headlineContent = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                    leadingContent = { Icon(if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                    modifier = Modifier.clickable { onArchiveToggle(note) }
                )
                ListItem(
                    headlineContent = { Text("Change color") },
                    leadingContent = { Icon(Icons.Outlined.Palette, null) },
                    modifier = Modifier.clickable { onColorPicker(note) }
                )
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDelete(note) }
                )
            } else {
                ListItem(
                    headlineContent = { Text("Empty Trash", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onEmptyTrash() }
                )
                ListItem(
                    headlineContent = { Text("Restore") },
                    leadingContent = { Icon(Icons.Default.Restore, null) },
                    modifier = Modifier.clickable { onRestore(note) }
                )
                ListItem(
                    headlineContent = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDeletePermanently(note) }
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Note?") },
        text = { Text("This action cannot be undone. The note will be permanently removed.") },
        icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onColorSelected: (NoteEntity, Color) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // Treat 0, White (light), or Dark Grey (dark) as the default state
    val isDefault = note.color == 0 || 
                    note.color == 0xFFFFFFFF.toInt() || 
                    note.color == 0xFF1F1F1F.toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                            onColorSelected(note, Color.Transparent) // Use Transparent (0) for default
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
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
                            if (isDefault) {
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
                items(colors) { color ->
                    Surface(
                        onClick = { onColorSelected(note, color) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = color,
                        modifier = Modifier.size(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        if (note.color == color.toArgb()) {
                            Icon(
                                Icons.Default.Check, 
                                null, 
                                modifier = Modifier.padding(12.dp), 
                                tint = if (color == Color.White || (isDark && color == Color(0xFFE8EAED))) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSearchResultItem(note: NoteEntity, onNoteClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(note.title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(NoteMetadataUtils.stripHtml(note.content), maxLines = 1) },
        trailingContent = { Text(DateUtils.formatTimestamp(note.createdAt), style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.clickable { onNoteClick(note.id) }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val isDark = isSystemInDarkTheme()
    // 0 or the default theme colors are treated as "Default"
    val isCustomColor = note.color != 0 && 
                        note.color != 0xFFFFFFFF.toInt() && 
                        note.color != 0xFF1F1F1F.toInt()

    val cardColor = if (isCustomColor) Color(note.color) else MaterialTheme.colorScheme.surface
    val contentColor = if (isCustomColor) ColorUtils.getContrastingColor(cardColor) else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = if (isCustomColor) ColorUtils.getSecondaryContrastingColor(cardColor) else MaterialTheme.colorScheme.onSurfaceVariant
    val tertiaryContentColor = if (isCustomColor) ColorUtils.getSecondaryContrastingColor(cardColor) else MaterialTheme.colorScheme.outline
    val noteShape = NoteShapes.getShapeForColor(cardColor)
    val bodyStyle = NoteShapes.getTextStyleForColor(cardColor, MaterialTheme.typography.bodyMedium)
    
    val context = LocalContext.current
    val smartActions = remember(note.content) { SmartContextUtils.extractActions(note.content) }

    val atmosphericBrush = if (isCustomColor) {
        Brush.linearGradient(
            colors = listOf(
                cardColor,
                cardColor.copy(alpha = 0.85f),
                cardColor.copy(alpha = 0.95f)
            )
        )
    } else null

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = noteShape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isCustomColor) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        border = if (note.isPinned) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.then(
            if (atmosphericBrush != null) Modifier.background(atmosphericBrush) else Modifier
        )) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                if (note.reminderTime != null && note.reminderTime > System.currentTimeMillis()) {
                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.reminderTime))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    Text(
                        text = NoteMetadataUtils.getHighlightedText(note.title, searchQuery, highlightColor),
                        style = NoteShapes.getTextStyleForColor(cardColor, MaterialTheme.typography.titleMedium),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        color = contentColor
                    )
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary 
                                   else secondaryContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (note.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = contentColor.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Locked Note",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (note.type == NoteType.TEXT) {
                    if (note.isLocked) {
                        // Don't show content for locked notes
                    } else if (note.content.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        Text(
                            text = NoteMetadataUtils.getHighlightedText(
                                NoteMetadataUtils.stripHtml(note.content),
                                searchQuery,
                                highlightColor
                            ),
                            style = bodyStyle,
                            maxLines = 6,
                            color = secondaryContentColor
                        )
                    }
                } else {
                    if (note.isLocked) {
                        // Don't show content for locked notes
                    } else if (note.checklistItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val checkedCount = note.checklistItems.count { it.isChecked }
                        val totalCount = note.checklistItems.size
                        
                        Text(
                            text = "$checkedCount/$totalCount items done",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryContentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        LinearProgressIndicator(
                            progress = { checkedCount.toFloat() / totalCount },
                            modifier = Modifier.fillMaxWidth().height(2.dp).padding(bottom = 8.dp),
                            color = contentColor.copy(alpha = 0.5f),
                            trackColor = contentColor.copy(alpha = 0.1f)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            note.checklistItems.take(4).forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = secondaryContentColor
                                    )
                                    Text(
                                        text = item.text,
                                        style = bodyStyle,
                                        maxLines = 1,
                                        color = if (item.isChecked) secondaryContentColor.copy(alpha = 0.6f) else secondaryContentColor,
                                        textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                }
                            }
                            if (note.checklistItems.size > 5) {
                                Text(
                                    text = "+ ${note.checklistItems.size - 5} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tertiaryContentColor,
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                        }
                    }
                }

                if (smartActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        smartActions.forEach { action ->
                            AssistChip(
                                onClick = {
                                    val intent = when (action.type) {
                                        SmartContextUtils.ActionType.URL -> Intent(Intent.ACTION_VIEW, Uri.parse(action.data))
                                        SmartContextUtils.ActionType.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", action.data, null))
                                        SmartContextUtils.ActionType.PHONE -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.data}"))
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {}
                                },
                                label = { Text(action.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(action.icon, null, modifier = Modifier.size(14.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = contentColor.copy(alpha = 0.1f),
                                    labelColor = contentColor,
                                    leadingIconContentColor = contentColor
                                ),
                                border = null
                            )
                        }
                    }
                }
                
                if (note.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        note.labels.forEach { label ->
                            SuggestionChip(
                                onClick = { /* Maybe filter by this label? */ },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = contentColor.copy(alpha = 0.7f),
                                    containerColor = contentColor.copy(alpha = 0.05f)
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = DateUtils.formatTimestamp(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = tertiaryContentColor
                )
            }
        }
    }
}

@Composable
fun EmptyState(searchQuery: String, filter: NoteFilter) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            if (searchQuery.isEmpty() && filter == NoteFilter.ALL) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(280.dp)
                )
            } else {
                val icon = when {
                    searchQuery.isNotEmpty() -> Icons.Default.SearchOff
                    filter == NoteFilter.ARCHIVED -> Icons.Default.Archive
                    filter == NoteFilter.PINNED -> Icons.Default.PushPin
                    filter == NoteFilter.DELETED -> Icons.Default.DeleteSweep
                    else -> Icons.Default.NoteAlt
                }
                
                // For Trash/Deleted, we could use a custom colored icon or a different visual
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = if (filter == NoteFilter.DELETED) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) 
                           else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            
            val titleText = when {
                searchQuery.isNotEmpty() -> "No matches found"
                filter == NoteFilter.ARCHIVED -> "Archive is empty"
                filter == NoteFilter.PINNED -> "No pinned notes"
                filter == NoteFilter.DELETED -> "Trash is empty"
                else -> "Your notes will appear here"
            }
            
            val subtitleText = when {
                searchQuery.isNotEmpty() -> "Try searching for something else"
                filter == NoteFilter.ARCHIVED -> "Notes you archive will be stored here"
                filter == NoteFilter.PINNED -> "Pin important notes to find them quickly"
                filter == NoteFilter.DELETED -> "Items in trash are automatically deleted after 30 days"
                else -> "Tap the + button to create your first note"
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
