package com.tzuhsien.immediat.data.model

data class Item(
    val contentDetails: ContentDetails,
    val etag: String,
    val id: String,
    val kind: String,
    val liveStreamingDetails: LiveStreamingDetails,
    val snippet: Snippet,
    val status: Status
)