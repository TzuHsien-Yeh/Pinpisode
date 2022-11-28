package com.tzuhsien.pinpisode.data.model

data class Item(
    val id: String,
    val etag: String,
    val kind: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails
)