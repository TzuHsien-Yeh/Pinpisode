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