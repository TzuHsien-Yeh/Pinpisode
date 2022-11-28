package com.tzuhsien.pinpisode.data.model

data class SpotifyShowResult(
    val href: String,
    val items: List<ShowItem>,
    val limit: Int,
    val total: Int,
    val error: Error?
)