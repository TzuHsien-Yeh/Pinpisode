package com.tzuhsien.immediat.ext

import com.google.firebase.firestore.FirebaseFirestore
import com.tzuhsien.immediat.data.model.YouTubeResult
import java.util.*

fun addYouTubeNoteData(result: YouTubeResult) {
    val youtubeNotes = FirebaseFirestore.getInstance()
        .collection("youtubeNotes")

    val document = youtubeNotes.document()

    val data = hashMapOf(
        "author" to hashMapOf(
            "email" to "wayne@school.appworks.tw",
            "id" to "waynechen323",
            "name" to "AKA小安老師"
        ),
        "createdTime" to Calendar.getInstance().timeInMillis,
        "id" to document.id,
        "videoId" to result.items[0].id,
        "thumbnail" to result.items[0].snippet.thumbnails.default.url,
        "videoTitle" to result.items[0].snippet.title,
        "publishAt" to result.items[0].snippet.publishedAt,
        "liveBroadcastContent" to result.items[0].snippet.liveBroadcastContent,
        "duration" to result.items[0].contentDetails.duration

        // Add live stream info +　condition

    )
    document.set(data)
}