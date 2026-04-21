package com.example.agenttest.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.agenttest.MainActivity
import com.example.agenttest.R
import com.example.agenttest.data.local.dao.NoteDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var noteDao: NoteDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            rescheduleAllReminders(context)
            return
        }

        val noteId = intent.getStringExtra("NOTE_ID") ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val note = noteDao.getNoteById(noteId)
            if (note == null || note.isDeleted || note.reminderTime == null) return@launch

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "lumina_reminders"

            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NOTE_ID", noteId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                noteId.hashCode(),
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText(note.title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(noteId.hashCode(), notification)
            
            // Clear reminder after showing
            noteDao.insertNote(note.copy(reminderTime = null))
        }
    }

    private fun rescheduleAllReminders(context: Context) {
        val reminderManager = com.example.agenttest.util.ReminderManager(context)
        CoroutineScope(Dispatchers.IO).launch {
            val notesWithReminders = noteDao.getNotesWithReminders()
            val currentTime = System.currentTimeMillis()
            notesWithReminders.forEach { note ->
                note.reminderTime?.let { time ->
                    if (time > currentTime) {
                        reminderManager.setReminder(note, time)
                    } else {
                        // Reminder time passed while device was off, maybe show it now or clear it
                        // For now, let's clear it to be safe
                        noteDao.insertNote(note.copy(reminderTime = null))
                    }
                }
            }
        }
    }
}