package com.tzuhsien.pinpisode.data.model

data class Show(
    val availableMarkets: List<String>,
    val copyrights: List<Any>,
    val description: String,
    val explicit: Boolean,
    val externalUrls: ExternalUrls,
    val href: String,
    val htmlDescription: String,
    val id: String,
    val images: List<Image>,
    val isExternallyHosted: Boolean,
    val languages: List<String>,
    val mediaType: String,
    var name: String,
    val publisher: String,
    val totalEpisodes: Int,
    val type: String,
    val uri: String
)