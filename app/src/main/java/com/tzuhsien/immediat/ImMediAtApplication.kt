package com.tzuhsien.immediat

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class ImMediAtApplication: Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: ImMediAtApplication? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        val ai: ApplicationInfo = applicationContext().packageManager
            .getApplicationInfo(applicationContext().packageName, PackageManager.GET_META_DATA)

        // Get api key
        val YT_API_KEY = ai.metaData["youtubeApiKey"].toString()
        const val YOUTUBE_PARAM_PART = "contentDetails, id, liveStreamingDetails, snippet, status"
    }

}