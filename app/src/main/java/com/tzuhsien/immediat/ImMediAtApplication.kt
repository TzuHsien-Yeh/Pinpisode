package com.tzuhsien.immediat

import android.app.Application

class ImMediAtApplication: Application() {
    companion object {
        const val YOUTUBE_PARAM_PART = "contentDetails, id, liveStreamingDetails, snippet, status"
    }
}