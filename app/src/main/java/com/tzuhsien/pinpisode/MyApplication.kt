package com.tzuhsien.pinpisode

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.util.ServiceLocator
import kotlin.properties.Delegates

const val NOTIFICATION_CHANNEL_ID = "spotify_note"
const val NOTIFICATION_CHANNEL_NAME = "Spotify Note"

class MyApplication: Application() {

    val repository: Repository
        get() = ServiceLocator.provideTasksRepository()

    companion object {
        var instance: MyApplication by Delegates.notNull()

        fun applicationContext() : Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            channel.setSound(null, null)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
}