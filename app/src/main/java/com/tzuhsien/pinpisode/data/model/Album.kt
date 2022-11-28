package com.tzuhsien.pinpisode.data.model

data class Album(
    val albumType: String,
    val artists: List<Artist>,
    val href: String,
    val id: String,
    val images: List<Image>,
    val name: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val totalTracks: Int,
    val type: String,
    val uri: String
)