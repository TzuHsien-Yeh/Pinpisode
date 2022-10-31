package com.tzuhsien.immediat.data.model

import com.google.gson.annotations.SerializedName

data class YouTubeResult(
    val error: String? = null,
    val etag: String,
    val items: List<Item>,
    val kind: String,
    val pageInfo: PageInfo
)