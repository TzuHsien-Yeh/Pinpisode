package com.tzuhsien.immediat.data.model

data class Episodes(
    val href: String,
    val items: List<SpotifyItem>,
    val total: Int
)