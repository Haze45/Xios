package com.yourapp.music.domain.usecase.profile

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.ProfileRepository
import javax.inject.Inject

class UploadProfilePhotoUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, bytes: ByteArray): Resource<String> =
        profileRepository.uploadProfilePhoto(userId, bytes)
}
