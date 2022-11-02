package com.tzuhsien.immediat.data.model

import com.tzuhsien.immediat.data.source.local.UserManager

data class Note (
    var id: String = "",
    val sourceId: String = "",
    val source: String = "",
    val ownerId: String = UserManager.userId,
    val authors: List<String> = listOf(UserManager.userId),
    val tags: List<String>  = listOf(),
    var lastEditTime: Long = -1,
    var digest: String = "",
    val isPublic: Boolean = false,
    val thumbnails: String = "",
    val title: String = ""
)