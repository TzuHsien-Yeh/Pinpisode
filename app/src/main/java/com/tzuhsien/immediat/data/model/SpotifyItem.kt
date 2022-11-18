package com.tzuhsien.immediat.data.model

data class SpotifyItem(
    val description: String,
    val durationMs: Int,
    val externalUrls: ExternalUrls,
    val id: String,
    val images: List<Image>,
    val isPlayable: Boolean,
    val language: String,
    val name: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val type: String,
    val uri: String,
    val album: Album? = null
)