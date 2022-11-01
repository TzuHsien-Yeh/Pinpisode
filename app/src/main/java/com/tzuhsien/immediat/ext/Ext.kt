package com.tzuhsien.immediat.ext

import java.text.SimpleDateFormat
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration


// Convert UTC to local time

fun String.utcToLocalTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val consultationDate = sdf.parse(this)?.toString() ?: ""

    return consultationDate
}

@OptIn(ExperimentalTime::class)
fun Float.convertDurationToDisplay(): String {
    val millis = this.toLong()
    val duration = millis.toDuration(DurationUnit.MILLISECONDS)
    val timeString =
        duration.toComponents { HH, MM, SS ->
            String.format("%02d:%02d:%02d", HH, MM, SS)
        }
    return timeString
}

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