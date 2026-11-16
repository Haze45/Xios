package com.yourapp.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourapp.music.data.local.dao.ArtistDao
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.dao.FavoriteDao
import com.yourapp.music.data.local.dao.PlaylistDao
import com.yourapp.music.data.local.dao.RecentSearchDao
import com.yourapp.music.data.local.dao.SongDao
import com.yourapp.music.data.local.entity.ArtistEntity
import com.yourapp.music.data.local.entity.DownloadedSongEntity
import com.yourapp.music.data.local.entity.FavoriteEntity
import com.yourapp.music.data.local.entity.PlaylistEntity
import com.yourapp.music.data.local.entity.RecentSearchEntity
import com.yourapp.music.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        FavoriteEntity::class,
        ArtistEntity::class,
        RecentSearchEntity::class,
        DownloadedSongEntity::class
    ],
    version = 6, // bumped: added downloaded_songs table
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun artistDao(): ArtistDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun downloadDao(): DownloadDao
}
