package com.tzuhsien.immediat

import android.app.Application
import android.content.Context

class ImMediAtApplication: Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: ImMediAtApplication? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        const val YOUTUBE_PARAM_PART = "contentDetails, id, liveStreamingDetails, snippet, status"
    }

}