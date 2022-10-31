package com.tzuhsien.immediat.ext

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.tzuhsien.immediat.data.model.ClipNote
import com.tzuhsien.immediat.data.model.TimestampNote
import com.tzuhsien.immediat.data.model.YouTubeResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

// Convert UTC to local time
fun String.toDate(dateFormat: String = "yyyy-MM-dd HH:mm:ss", timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date {
    val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
    parser.timeZone = timeZone
    return parser.parse(this)
}

fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = timeZone
    return formatter.format(this)
}


//
//private val youtubeNotes = FirebaseFirestore.getInstance()
//    .collection("youtubeNotes")
//
//fun addYouTubeNoteData(result: YouTubeResult) {
//
//    val document = youtubeNotes.document()
//
//    val data = hashMapOf(
//        "author" to hashMapOf(
//            "email" to "wayne@school.appworks.tw",
//            "id" to "waynechen323",
//            "name" to "AKA小安老師"
//        ),
//        "createdTime" to Calendar.getInstance().timeInMillis,
//        "id" to document.id,
//        "videoId" to result.items[0].id,
//        "thumbnail" to result.items[0].snippet.thumbnails.default.url,
//        "videoTitle" to result.items[0].snippet.title,
//        "publishAt" to result.items[0].snippet.publishedAt,
//        "liveBroadcastContent" to result.items[0].snippet.liveBroadcastContent,
//        "lastEditTime" to Calendar.getInstance().timeInMillis,
//
//    )
//    document.set(data)
//}
//
//fun updateYouTubeNote(videoId: String, timestampNote: List<TimestampNote?>, clipNote: List<ClipNote?>){
//    val videoRef = youtubeNotes.document(videoId)
//
//    // Set the fields of the videoId
//    videoRef
//        .update(
//            "timestampNote", timestampNote,
//            "clipNote", clipNote,
//            "lastEditTime", Calendar.getInstance().timeInMillis
//        )
//        .addOnSuccessListener {
//            Timber.d ( "DocumentSnapshot successfully updated.")
//            //TODO: snapshot the firebase db to realtime update list to submit to recyclerview
//        }
//        .addOnFailureListener { e -> Timber.w("Error updating document", e) }
//
//}