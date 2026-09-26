package com.nur.quran.shared

data class TajweedSegment(
    val start: Int,
    val end: Int,
    val colorHex: String,
    val ruleClass: String? = null
)
