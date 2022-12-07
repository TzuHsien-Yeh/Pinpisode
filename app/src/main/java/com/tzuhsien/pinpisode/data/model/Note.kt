package com.tzuhsien.pinpisode.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note (
    var id: String = "",
    var sourceId: String = "",
    var source: String = "",
    var ownerId: String = "",
    var authors: List<String> = listOf(),
    var tags: List<String>  = listOf(),
    var lastEditTime: Long = -1,
    var digest: String = "",
    val isPublic: Boolean = false,
    var thumbnail: String = "",
    var title: String = "",
    var duration: String = "P0D",
    var lastTimestamp: Float = 0F
): Parcelable