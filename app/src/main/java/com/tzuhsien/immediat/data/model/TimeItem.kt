package com.tzuhsien.immediat.data.model

data class TimeItem(
    var id: String = "",
    var title: String = "",
    val startAt: Float = -1f,
    val endAt: Float? = null,
    var text: String = "",
    val img: String = ""
)