package com.tzuhsien.pinpisode.data.model

data class Episodes(
    val href: String,
    val items: List<SpotifyItem>,
    val total: Int
)