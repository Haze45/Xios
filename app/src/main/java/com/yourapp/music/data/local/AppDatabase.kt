package com.yourapp.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourapp.music.data.local.dao.ArtistDao
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.dao.FavoriteDao
import com.yourapp.music.data.local.dao.ListeningHistoryDao
import com.yourapp.music.data.local.dao.PlaylistDao
import com.yourapp.music.data.local.dao.RecentSearchDao
import com.yourapp.music.data.local.dao.SongDao
import com.yourapp.music.data.local.entity.ArtistEntity
import com.yourapp.music.data.local.entity.DownloadedSongEntity
import com.yourapp.music.data.local.entity.FavoriteEntity
import com.yourapp.music.data.local.entity.ListeningHistoryEntity
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
        DownloadedSongEntity::class,
        ListeningHistoryEntity::class
    ],
    version = 9,
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

    abstract fun listeningHistoryDao(): ListeningHistoryDao

    companion object {

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_history (
                        sessionId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        songId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        albumArtUrl TEXT NOT NULL,
                        playedAt INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        listenedMs INTEGER NOT NULL,
                        lastPositionMs INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(sessionId)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_listening_history_userId_playedAt
                    ON listening_history(userId, playedAt)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_listening_history_songId
                    ON listening_history(songId)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove records for the previous public, unencrypted downloads.
                db.execSQL("DELETE FROM downloaded_songs")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Previous encrypted files used the single-block format.
                // They must be downloaded again using chunked encryption.
                db.execSQL("DELETE FROM downloaded_songs")
            }
        }
    }
}
