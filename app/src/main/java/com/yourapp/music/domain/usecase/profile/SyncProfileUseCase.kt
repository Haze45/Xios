package com.yourapp.music.domain.usecase.profile

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Pushes the counts this app actually has real data for (playlists
 * created, artists followed) into Supabase's profiles table. Call this
 * on registration (to create the row) and whenever those counts change
 * (to keep it truthful) — otherwise the row either never exists or
 * silently drifts out of date, since nothing else ever writes to it.
 */
class SyncProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, followingCount: Int, playlistCount: Int): Resource<Unit> =
        profileRepository.syncProfileCounts(userId, followingCount, playlistCount)
}
