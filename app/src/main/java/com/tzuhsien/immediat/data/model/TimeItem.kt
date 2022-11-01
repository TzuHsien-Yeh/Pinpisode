package com.tzuhsien.immediat.data.model

data class TimeItem(
    val title: String = "",
    val startAt: Float = -1f,
    val endAt: Float? = null,
    val text: String = "",
    val img: String = ""
)