package com.yourapp.music.core.navigation

import androidx.lifecycle.ViewModel
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Decides which screen the app should open on, checked once at process
 * start. Firebase Auth persists the signed-in session locally on-device
 * by default, so getCurrentUserUseCase() here is a fast local check
 * (no network call) — if it returns a user, we skip straight past the
 * Login screen instead of forcing the person to log in every launch.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    val startDestination: String =
        if (getCurrentUserUseCase() != null) "main" else Screen.Login.route
}
