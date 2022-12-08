package com.tzuhsien.pinpisode

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.util.ServiceLocator
import kotlin.properties.Delegates

class MyApplication: Application() {

    val repository: Repository
        get() = ServiceLocator.provideTasksRepository(this)

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
                "spotify_note",
                "Spotify Note",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.setSound(null, null)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
}