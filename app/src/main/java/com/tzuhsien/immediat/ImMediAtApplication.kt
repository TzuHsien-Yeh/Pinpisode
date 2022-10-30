package com.tzuhsien.immediat

import android.app.Application
import android.content.Context
import kotlin.properties.Delegates

class ImMediAtApplication: Application() {

    companion object {
        var instance: ImMediAtApplication by Delegates.notNull()

        fun applicationContext() : Context {
            return instance.applicationContext
        }

        const val YOUTUBE_PARAM_PART = "contentDetails, id, liveStreamingDetails, snippet, status"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}