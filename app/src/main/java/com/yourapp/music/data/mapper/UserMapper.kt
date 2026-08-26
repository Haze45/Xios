package com.yourapp.music.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.yourapp.music.domain.model.User

fun FirebaseUser.toDomain(): User = User(
    uid = uid,
    email = email.orEmpty(),
    displayName = displayName,
    photoUrl = photoUrl?.toString()
)
