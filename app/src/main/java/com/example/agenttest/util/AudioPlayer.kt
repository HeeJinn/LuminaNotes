package com.example.agenttest.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(path: String) {
        stopAudio()
        mediaPlayer = MediaPlayer().apply {
            if (path.startsWith("/")) {
                setDataSource(path)
            } else {
                setDataSource(context, Uri.parse(path))
            }
            prepare()
            start()
        }
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
