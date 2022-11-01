package com.tzuhsien.immediat.data.model

import android.icu.text.CaseMap

data class Note (
    val id: String, // YouTube or Spotify source id
    val source: String,
    val ownerId: String,
    val authors: List<String>,
    val tags: List<String>,
    var lastEditTime: Long = -1,
    val digest: String = "",
    val isPublic: Boolean = false,
    val thumbnails: String,
    val title: String
)