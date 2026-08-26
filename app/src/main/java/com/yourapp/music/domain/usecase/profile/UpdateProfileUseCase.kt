package com.yourapp.music.domain.usecase.profile

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Updates the signed-in user's display name and/or profile photo.
 * Pass null for either parameter to leave it unchanged.
 */
class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(displayName: String?, photoUri: String?): Resource<User> =
        authRepository.updateProfile(displayName, photoUri)
}
