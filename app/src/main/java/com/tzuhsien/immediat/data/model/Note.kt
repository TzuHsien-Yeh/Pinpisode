package com.tzuhsien.immediat.data.model

import android.os.Parcelable
import com.tzuhsien.immediat.data.source.local.UserManager
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note (
    var id: String = "",
    val sourceId: String = "",
    val source: String = "",
    val ownerId: String = UserManager.userId,
    val authors: List<String> = listOf(UserManager.userId),
    var tags: List<String>  = listOf(),
    var lastEditTime: Long = -1,
    var digest: String = "",
    val isPublic: Boolean = false,
    val thumbnails: String = "",
    val title: String = "",
    val duration: String = "",
    var lastTimestamp: Float = 0F
): Parcelable