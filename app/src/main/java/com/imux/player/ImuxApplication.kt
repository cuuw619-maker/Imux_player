package com.imux.player

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.imux.player.data.*
import com.imux.player.media.ArtworkLoader
import com.imux.player.media.SupportedAudioFormatRegistry
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors

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

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.error("UNCAUGHT", "Fatal exception on " + thread.name, throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        startMainThreadWatchdog()
    }

    private fun startMainThreadWatchdog() {
        val main = Handler(Looper.getMainLooper())
        val worker = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "Imux-Watchdog").apply { isDaemon = true }
        }
        worker.execute {
            var lastBeat = SystemClock.uptimeMillis()
            while (true) {
                Thread.sleep(1000)
                val postedAt = SystemClock.uptimeMillis()
                main.post { lastBeat = SystemClock.uptimeMillis() }
                if (postedAt - lastBeat >= 5000) {
                    AppLogger.error(
                        "ANR_WATCHDOG",
                        "Main thread appears blocked for at least " + (postedAt - lastBeat) + " ms."
                    )
                    lastBeat = postedAt
                }
            }
        }
    }
}

object AppLogger {
    private const val TAG = "ImuxPlayer"
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Imux-Logger").apply { isDaemon = true }
    }
    @Volatile private var file: File? = null

    fun init(context: android.content.Context) {
        file = File(context.filesDir, "imux.log")
        info("SYSTEM", "Logger initialized")
    }

    fun info(scope: String, message: String) = write("INFO", scope, message, null)
    fun error(scope: String, message: String, throwable: Throwable? = null) =
        write("ERROR", scope, message, throwable)

    private fun write(level: String, scope: String, message: String, throwable: Throwable?) {
        val line = buildString {
            append(System.currentTimeMillis())
            append(" [").append(level).append("] ")
            append(scope).append(": ").append(message)
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append("\n").append(sw)
            }
        }
        Log.println(if (level == "ERROR") Log.ERROR else Log.INFO, TAG, line)
        executor.execute { runCatching { file?.appendText(line + "\n") } }
    }
}
