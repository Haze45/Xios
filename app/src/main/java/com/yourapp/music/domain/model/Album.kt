package com.yourapp.music.domain.model

/**
 * Not a stored/user-created entity — purely a grouping of the existing
 * song catalog by their `album` tag, computed on the fly. coverUrl
 * always reflects the LAST song in that album (by catalog order), same
 * "most recently added wins" rule used for playlist covers.
 */
data class Album(
    val id: String, // the album name itself, used as the identifier since there's no separate albums table
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val songIds: List<String>
) {
    val songCount: Int get() = songIds.size
}
