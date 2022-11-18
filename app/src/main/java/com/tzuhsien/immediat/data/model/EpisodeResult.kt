package com.tzuhsien.immediat.data.model

data class EpisodeResult(
    val audioPreviewUrl: String,
    val description: String,
    val durationMs: Int,
    val explicit: Boolean,
    val externalUrls: ExternalUrls,
    val href: String,
    val htmlDescription: String,
    val id: String,
    val images: List<Image>,
    val isExternallyHosted: Boolean,
    val isPlayable: Boolean,
    val language: String,
    val languages: List<String>,
    val name: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val show: Show,
    val type: String,
    val uri: String
)