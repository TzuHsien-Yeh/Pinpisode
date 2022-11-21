package com.tzuhsien.immediat.data.model

data class YouTubeSearchResult(
    val etag: String,
    val items: List<ItemX>,
    val kind: String,
    val nextPageToken: String,
    val pageInfo: PageInfo,
    val regionCode: String
)