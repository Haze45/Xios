package com.yourapp.music.domain.model

data class Genre(
    val id: String,
    val name: String,
    val colorHex: String // e.g. "#FF6B6B" — used for the genre tile background
)
