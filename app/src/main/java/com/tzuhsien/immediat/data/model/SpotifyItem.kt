package com.tzuhsien.immediat.data.model

data class SpotifyItem(
    val description: String = "",
    val durationMs: Int = 0,
    val id: String = "",
    val images: List<Image> = listOf(),
    val isPlayable: Boolean = true,
    val language: String = "",
    val name: String = "",
    val releaseDate: String = "",
    val releaseDatePrecision: String = "",
    var show: Show? = null,
    val type: String = "",
    val uri: String = "",
    val album: Album? = null
)