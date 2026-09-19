package com.imux.player.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0,
    val favorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val playCount: Int = 0,
    val position: Long = 0,
    val mime: String = "",
    val size: Long = 0
)

@Entity(tableName = "folders")
data class Folder(@PrimaryKey val uri: String, val name: String)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "trackUri"],
    indices = [Index("playlistId"), Index("trackUri")]
)
data class PlaylistItem(
    val playlistId: Long,
    val trackUri: String,
    val position: Int
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun all(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(value: List<Track>)

    @Query("UPDATE tracks SET favorite=:value WHERE uri=:uri")
    suspend fun favorite(uri: String, value: Boolean)

    @Query("UPDATE tracks SET lastPlayed=:time,playCount=playCount+1 WHERE uri=:uri")
    suspend fun played(uri: String, time: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(value: Folder)

    @Query("DELETE FROM folders WHERE uri=:uri")
    suspend fun remove(uri: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun all(): Flow<List<Playlist>>

    @Insert
    suspend fun create(value: Playlist): Long

    @Query("UPDATE playlists SET name=:name,updatedAt=:updatedAt WHERE id=:id")
    suspend fun rename(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id=:id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(value: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId=:playlistId AND trackUri=:trackUri")
    suspend fun removeItem(playlistId: Long, trackUri: String)

    @Query("DELETE FROM playlist_items WHERE playlistId=:playlistId")
    suspend fun clearItems(playlistId: Long)

    @Query("SELECT trackUri FROM playlist_items WHERE playlistId=:playlistId ORDER BY position")
    suspend fun trackUris(playlistId: Long): List<String>
}

@Database(entities = [Track::class, Folder::class, Playlist::class, PlaylistItem::class], version = 2)
abstract class ImuxDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun folders(): FolderDao
    abstract fun playlists(): PlaylistDao
}
