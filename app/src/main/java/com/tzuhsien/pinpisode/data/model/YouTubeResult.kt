package com.tzuhsien.pinpisode.data.model

data class YouTubeResult(
    val etag: String,
    val items: List<Item>,
    val kind: String,
    val pageInfo: PageInfo
)