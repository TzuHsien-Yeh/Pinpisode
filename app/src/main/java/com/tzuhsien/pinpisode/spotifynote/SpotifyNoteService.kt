package com.tzuhsien.pinpisode.spotifynote

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat


class SpotifyNoteService : Service() {

    private var isClipping = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_CLIPPING = "ACTION_START_CLIPPING"
        const val ACTION_DONE_CLIPPING = "ACTION_DONE_CLIPPING"
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_START_CLIPPING -> startClipping()
            ACTION_DONE_CLIPPING -> doneClipping()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        NotificationBuilder().build(this, isClipping, channelId = "spotify_note", packageName = packageName)
        startForeground(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        return NotificationBuilder().build(this, isClipping, channelId = "spotify_note", packageName = packageName)
    }

    private fun startClipping() {
        isClipping = true
        with(NotificationManagerCompat.from(this)) {
            notify(1, buildNotification())
        }
    }

    private fun doneClipping() {
        isClipping = false
        with(NotificationManagerCompat.from(this)) {
            notify(1, buildNotification())
        }
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }
}