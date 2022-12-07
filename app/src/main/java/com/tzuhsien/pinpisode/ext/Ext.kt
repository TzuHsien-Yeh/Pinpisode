package com.tzuhsien.pinpisode.ext

import android.text.format.DateUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tzuhsien.pinpisode.R
import java.text.SimpleDateFormat
import kotlin.time.Duration


// Convert UTC to local time
fun String.utcToLocalTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    return sdf.parse(this)?.toString() ?: ""
}

fun Float.formatDuration(): String = DateUtils.formatElapsedTime(this.toLong())

fun Long.formatDuration(): String = DateUtils.formatElapsedTime(this / 1000)

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

fun String.extractSpotifySourceId(): String {
    val spotifyShareLink = "https://open.spotify.com/"
    val spotifyUri = "spotify:"

    return if (spotifyShareLink in this) {
        this.substringAfter(spotifyShareLink)
            .substringBefore("?si=", this.substringAfter(spotifyShareLink))
            .replace("/", ":")

    } else {
        this.substringAfter(spotifyUri)
    }
}

fun String.parseSpotifyImageUri(): String {
    val spotifyImageUri = "spotify:image:"
    val imgHttpsUri = "https://i.scdn.co/image/"

    return imgHttpsUri + this.substringAfter(spotifyImageUri)
}

fun ImageView.glide(uri: String?) {
    Glide.with(this)
        .load(uri)
        .apply(
            RequestOptions
                .placeholderOf(R.drawable.app_icon)
                .error(R.drawable.app_icon)
        )
        .into(this)
}