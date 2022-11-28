package com.tzuhsien.pinpisode.data.model

import android.os.Parcelable
import com.tzuhsien.pinpisode.data.source.local.UserManager
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note (
    var id: String = "",
    var sourceId: String = "",
    var source: String = "",
    val ownerId: String = UserManager.userId!!,
    val authors: List<String> = listOf(UserManager.userId!!),
    var tags: List<String>  = listOf(),
    var lastEditTime: Long = -1,
    var digest: String = "",
    val isPublic: Boolean = false,
    var thumbnail: String = "",
    var title: String = "",
    var duration: String = "",
    var lastTimestamp: Float = 0F
): Parcelable