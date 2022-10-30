package com.tzuhsien.immediat.data.model

data class ClipNote(
    val clipTitle: String?,
    val startTime: Float,
    val endTime: Float,
    val textContent: String?,
    val bulletPoint: List<String?>?
)
