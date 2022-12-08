package com.tzuhsien.pinpisode.ext

import android.text.format.DateUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tzuhsien.pinpisode.Constants
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
    return if (Constants.YOUTUBE_WATCH_URL in this) {
        this
            .substringAfter(Constants.YOUTUBE_WATCH_URL)
            .substringBefore("&", this.substringAfter(Constants.YOUTUBE_WATCH_URL))
    } else {
        this.substringAfter(Constants.YOUTUBE_SHARE_LINK)
    }
}

fun String.extractSpotifySourceId(): String {
    return if (Constants.SPOTIFY_SHARE_LINK in this) {
        this.substringAfter(Constants.SPOTIFY_SHARE_LINK)
            .substringBefore("?si=", this.substringAfter(Constants.SPOTIFY_SHARE_LINK))
            .replace("/", ":")
    } else {
        this.substringAfter(Constants.SPOTIFY_URI)
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