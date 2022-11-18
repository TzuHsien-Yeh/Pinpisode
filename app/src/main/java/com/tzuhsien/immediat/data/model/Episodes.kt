package com.tzuhsien.immediat.data.model

data class Episodes(
    val href: String,
    val items: List<ItemXX>,
    val limit: Int,
    val next: String,
    val previous: String,
    val total: Int
)