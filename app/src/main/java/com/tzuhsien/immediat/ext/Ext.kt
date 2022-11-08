package com.tzuhsien.immediat.ext

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import kotlin.time.Duration


// Convert UTC to local time
fun String.utcToLocalTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val consultationDate = sdf.parse(this)?.toString() ?: ""

    return consultationDate
}

fun Float.formatDuration(): String = DateUtils.formatElapsedTime(this.toLong())

fun String.parseDuration(): Long? = Duration.parseIsoStringOrNull(this)?.inWholeMilliseconds


fun String.extractYoutubeVideoId(): String {
    val youtubeWatchUrl = "youtube.com/watch?v="
    val youtubeShareLink = "youtu.be/"

    return if (youtubeWatchUrl in this) {
        this
            .substringAfter(youtubeWatchUrl)
            .substringBefore("&", this.substringAfter(youtubeWatchUrl))
    } else {
        this.substringAfter(youtubeShareLink)
    }
}