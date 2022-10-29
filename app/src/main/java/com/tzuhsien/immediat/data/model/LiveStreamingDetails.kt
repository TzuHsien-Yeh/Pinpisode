package com.tzuhsien.immediat.data.model

data class LiveStreamingDetails(
    val activeLiveChatId: String,
    val actualStartTime: String,
    val concurrentViewers: String,
    val scheduledStartTime: String
)