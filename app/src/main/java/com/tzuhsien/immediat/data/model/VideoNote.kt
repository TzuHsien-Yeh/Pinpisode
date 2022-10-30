package com.tzuhsien.immediat.data.model

data class VideoNote (
    val id: String,
    val youTubeResult: YouTubeResult,
    val timestampNote: List<TimestampNote>,
    val clipNote: List<ClipNote>
)