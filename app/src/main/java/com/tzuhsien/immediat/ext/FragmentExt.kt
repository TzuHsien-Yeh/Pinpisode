package com.tzuhsien.immediat.ext

import androidx.fragment.app.Fragment
import com.tzuhsien.immediat.ImMediAtApplication.Companion.applicationContext
import com.tzuhsien.immediat.factory.YoutubeVideoViewModelFactory


fun Fragment.getVmFactory(videoId: String): YoutubeVideoViewModelFactory {
//    val repository = applicationContext().repository
    return YoutubeVideoViewModelFactory(videoId)
}