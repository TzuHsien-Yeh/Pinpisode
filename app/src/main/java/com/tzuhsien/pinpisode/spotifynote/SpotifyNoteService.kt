package com.tzuhsien.pinpisode.spotifynote

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.tzuhsien.pinpisode.NOTIFICATION_CHANNEL_ID
import timber.log.Timber

private const val NOTIFICATION_ID = 1

class SpotifyNoteService : Service() {

    private var isClipping = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_CLIPPING = "ACTION_START_CLIPPING"
        const val ACTION_DONE_CLIPPING = "ACTION_DONE_CLIPPING"
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
        startForeground(NOTIFICATION_ID, buildNotification())
        Timber.d("start: startForeground")
    }

    private fun buildNotification(): Notification {
        Timber.d("buildNotification")
        return NotificationBuilder().build(this, isClipping, channelId = NOTIFICATION_CHANNEL_ID, packageName = packageName)
    }

    private fun startClipping() {
        isClipping = true
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun doneClipping() {
        isClipping = false
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun stop() {
        Timber.d("stop")
        stopForeground(true)
        stopSelf()
    }
}