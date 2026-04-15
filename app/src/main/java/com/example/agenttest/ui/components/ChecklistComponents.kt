package com.example.agenttest.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.agenttest.data.local.entity.ChecklistItem
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

@Composable
fun ChecklistEditor(
    items: MutableList<ChecklistItem>,
    contentColor: Color,
    onItemsChanged: () -> Unit
) {
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var itemToFocus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(itemToFocus) {
        itemToFocus?.let { id ->
            focusRequesters[id]?.requestFocus()
            itemToFocus = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val uncheckedItems = items.filter { !it.isChecked }
        val checkedItems = items.filter { it.isChecked }

        uncheckedItems.forEachIndexed { index, item ->
            val focusRequester = focusRequesters.getOrPut(item.id) { FocusRequester() }
            ChecklistItemRow(
                item = item,
                contentColor = contentColor,
                focusRequester = focusRequester,
                showReorderButtons = true,
                canMoveUp = index > 0,
                canMoveDown = index < uncheckedItems.size - 1,
                onCheckedChange = { isChecked ->
                    val idx = items.indexOf(item)
                    if (idx != -1) {
                        items[idx] = item.copy(isChecked = isChecked)
                        onItemsChanged()
                    }
                },
                onTextChange = { text ->
                    val idx = items.indexOf(item)
                    if (idx != -1) {
                        items[idx] = item.copy(text = text)
                        onItemsChanged()
                    }
                },
                onRemove = {
                    items.remove(item)
                    focusRequesters.remove(item.id)
                    onItemsChanged()
                },
                onEnterPressed = {
                    val idx = items.indexOf(item)
                    val newItem = ChecklistItem(text = "")
                    items.add(idx + 1, newItem)
                    itemToFocus = newItem.id
                    onItemsChanged()
                },
                onMoveUp = {
                    val idx = items.indexOf(item)
                    if (idx > 0) {
                        val prevItem = items[idx - 1]
                        items[idx - 1] = items[idx]
                        items[idx] = prevItem
                        onItemsChanged()
                    }
                },
                onMoveDown = {
                    val idx = items.indexOf(item)
                    if (idx < items.size - 1) {
                        val nextItem = items[idx + 1]
                        items[idx + 1] = items[idx]
                        items[idx] = nextItem
                        onItemsChanged()
                    }
                }
            )
        }

        // Add Item Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    val newItem = ChecklistItem(text = "")
                    items.add(newItem)
                    itemToFocus = newItem.id
                    onItemsChanged()
                }
                .padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = contentColor.copy(alpha = 0.6f))
            Spacer(Modifier.width(12.dp))
            Text("List item", color = contentColor.copy(alpha = 0.6f))
        }

        if (checkedItems.isNotEmpty()) {
            var expanded by remember { mutableStateOf(true) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${checkedItems.size} Checked items",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.None
                    )
                }
                
                IconButton(
                    onClick = {
                        items.removeAll { it.isChecked }
                        onItemsChanged()
                    }
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear checked items",
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                checkedItems.forEach { item ->
                    ChecklistItemRow(
                        item = item,
                        contentColor = contentColor.copy(alpha = 0.4f),
                        onCheckedChange = { isChecked ->
                            val index = items.indexOf(item)
                            if (index != -1) {
                                items[index] = item.copy(isChecked = isChecked)
                                onItemsChanged()
                            }
                        },
                        onTextChange = { text ->
                            val index = items.indexOf(item)
                            if (index != -1) {
                                items[index] = item.copy(text = text)
                                onItemsChanged()
                            }
                        },
                        onRemove = {
                            items.remove(item)
                            onItemsChanged()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    contentColor: Color,
    focusRequester: FocusRequester? = null,
    showReorderButtons: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    onEnterPressed: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        if (showReorderButtons) {
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDropUp, 
                        contentDescription = "Move Up", 
                        tint = contentColor.copy(alpha = if (canMoveUp) 0.6f else 0.1f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDropDown, 
                        contentDescription = "Move Down", 
                        tint = contentColor.copy(alpha = if (canMoveDown) 0.6f else 0.1f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Icon(
                Icons.Default.DragIndicator, 
                contentDescription = null, 
                tint = contentColor.copy(alpha = 0.3f), 
                modifier = Modifier.size(20.dp).padding(start = 4.dp)
            )
        }
        
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = contentColor.copy(alpha = 0.6f),
                uncheckedColor = contentColor,
                checkmarkColor = Color.White
            )
        )
        
        TextField(
            value = item.text,
            onValueChange = onTextChange,
            placeholder = { Text("List item", color = contentColor.copy(alpha = 0.3f)) },
            modifier = Modifier
                .weight(1f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onKeyEvent { 
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        onEnterPressed?.invoke()
                        true
                    } else {
                        false
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = contentColor,
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ChecklistViewer(
    items: List<ChecklistItem>,
    contentColor: Color,
    onToggleItem: ((ChecklistItem) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val uncheckedItems = items.filter { !it.isChecked }
        val checkedItems = items.filter { it.isChecked }

        uncheckedItems.forEach { item ->
            ChecklistDisplayRow(item, contentColor, onToggleItem)
        }

        if (checkedItems.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${checkedItems.size} Checked items",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.5f)
                )
            }

            if (expanded) {
                checkedItems.forEach { item ->
                    ChecklistDisplayRow(item, contentColor.copy(alpha = 0.6f), onToggleItem)
                }
            }
        }
    }
}

@Composable
fun ChecklistDisplayRow(
    item: ChecklistItem,
    contentColor: Color,
    onToggleItem: ((ChecklistItem) -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onToggleItem != null) { onToggleItem?.invoke(item) }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = contentColor.copy(alpha = if (item.isChecked) 0.5f else 1.0f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = if (item.isChecked) 0.5f else 1.0f),
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
    }
}
