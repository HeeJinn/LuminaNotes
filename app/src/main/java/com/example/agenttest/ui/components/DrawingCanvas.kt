package com.example.agenttest.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import com.example.agenttest.util.VoiceRecorder
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DrawingDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var paths by remember { mutableStateOf(listOf<PathData>()) }
    var currentPath by remember { mutableStateOf<PathData?>(null) }
    val strokeColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(500.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Quick Sketch",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = PathData(Path().apply { moveTo(offset.x, offset.y) })
                                },
                                onDrag = { change, _ ->
                                    currentPath?.path?.lineTo(change.position.x, change.position.y)
                                    // Trigger recomposition
                                    val temp = currentPath
                                    currentPath = null
                                    currentPath = temp
                                },
                                onDragEnd = {
                                    currentPath?.let { paths = paths + it }
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { pathData ->
                            drawContext.canvas.nativeCanvas.drawPath(
                                pathData.path,
                                Paint().apply {
                                    color = strokeColor.toArgb()
                                    style = Paint.Style.STROKE
                                    strokeWidth = 10f
                                    strokeCap = Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                            )
                        }
                        currentPath?.let { pathData ->
                            drawContext.canvas.nativeCanvas.drawPath(
                                pathData.path,
                                Paint().apply {
                                    color = strokeColor.toArgb()
                                    style = Paint.Style.STROKE
                                    strokeWidth = 10f
                                    strokeCap = Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val bitmap = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        val paint = Paint().apply {
                            color = strokeColor.toArgb()
                            style = Paint.Style.STROKE
                            strokeWidth = 20f
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
                        paths.forEach { canvas.drawPath(it.path, paint) }
                        onSave(bitmap)
                    }) {
                        Text("Add to Note")
                    }
                }
            }
        }
    }
}

data class PathData(val path: Path)

@Composable
fun RecordingDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0L) }
    val amplitudes = remember { mutableStateListOf<Float>() }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(100)
                duration += 100
                amplitudes.add(recorder.getAmplitude())
                if (amplitudes.size > 50) amplitudes.removeAt(0)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Memo") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val centerY = size.height / 2
                        val spacing = size.width / 50
                        amplitudes.forEachIndexed { index, amp ->
                            val height = (amp / 32767f) * size.height
                            drawLine(
                                color = Color.Red,
                                start = Offset(index * spacing, centerY - height / 2),
                                end = Offset(index * spacing, centerY + height / 2),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", (duration / 1000) / 60, (duration / 1000) % 60),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isRecording) {
                        val file = recorder.stopRecording()
                        isRecording = false
                        file?.let { onSave(it.absolutePath) }
                        onDismiss()
                    } else {
                        recorder.startRecording()
                        isRecording = true
                    }
                }
            ) {
                Text(if (isRecording) "Stop & Save" else "Start Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

