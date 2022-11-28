package com.tzuhsien.pinpisode.data.model

data class Tracks(
    val href: String,
    val items: List<SpotifyItem>,
    val offset: Int,
    val total: Int
)