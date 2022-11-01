package com.tzuhsien.immediat.ext

import java.text.SimpleDateFormat


// Convert UTC to local time

fun String.utcToLocalTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val consultationDate = sdf.parse(this)?.toString() ?: ""

    return consultationDate
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