package com.yourapp.music.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null
)
