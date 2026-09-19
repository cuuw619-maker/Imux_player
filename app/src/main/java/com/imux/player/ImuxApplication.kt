package com.imux.player

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.imux.player.data.*
import com.imux.player.media.ArtworkLoader
import com.imux.player.media.SupportedAudioFormatRegistry

class ImuxApplication : Application() {
    private val migration1to2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_items (playlistId INTEGER NOT NULL, trackUri TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(playlistId, trackUri))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_items_playlistId ON playlist_items(playlistId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_items_trackUri ON playlist_items(trackUri)")
        }
    }

    val db by lazy {
        Room.databaseBuilder(this, ImuxDatabase::class.java, "imux.db")
            .addMigrations(migration1to2)
            .build()
    }
    val settings by lazy { SettingsRepository(this) }
    val formats by lazy { SupportedAudioFormatRegistry(settings) }
    val library by lazy { LibraryRepository(this, db, formats) }
    val artwork by lazy { ArtworkLoader(this) }
}
