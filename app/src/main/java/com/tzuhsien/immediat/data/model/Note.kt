package com.tzuhsien.immediat.data.model

data class Note (
    val id: String, // YouTube or Spotify source id
    val source: String,
    val ownerId: String,
    val authors: List<String>,
    val tags: List<String>,
    val timestampNotes: List<TimestampNote>,
    val clipNotes: List<ClipNote>,
    val lastEditTime: Long,
    val digest: String,
    val isPublic: Boolean
)