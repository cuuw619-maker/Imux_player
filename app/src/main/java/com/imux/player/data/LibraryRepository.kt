package com.imux.player.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.imux.player.AppLogger
import androidx.documentfile.provider.DocumentFile
import com.imux.player.media.SupportedAudioFormatRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val context: Context,
    private val db: ImuxDatabase,
    private val registry: SupportedAudioFormatRegistry
) {
    val tracks = db.tracks().all()
    val folders = db.folders().all()

    suspend fun addFolder(uri: String) = withContext(Dispatchers.IO) {
        val parsedUri = runCatching { Uri.parse(uri) }.getOrElse {
            error("Android returned an invalid folder URI.")
        }

        if (parsedUri.scheme != "content" || !DocumentsContract.isTreeUri(parsedUri)) {
            error("Android returned an invalid music folder.")
        }

        AppLogger.info("LIBRARY", "Persisting read access for folder: $parsedUri")
        try {
            context.contentResolver.takePersistableUriPermission(
                parsedUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (security: SecurityException) {
            AppLogger.error("LIBRARY", "Could not persist folder permission", security)
            error("Android did not grant persistent access to the selected folder.")
        } catch (unsupported: Exception) {
            AppLogger.error("LIBRARY", "Unexpected permission error", unsupported)
            error("The selected folder cannot be stored for later access.")
        }

        // Do not touch DocumentFile or enumerate the provider here. Some OEM
        // DocumentsProviders block for a long time. Registration must stay cheap;
        // scanning happens separately on IO in scanAll().
        val name = parsedUri.lastPathSegment
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
            ?: "Music"

        db.folders().add(Folder(uri, name))
        AppLogger.info("LIBRARY", "Folder stored: $parsedUri")
    }

    suspend fun scanAll() = withContext(Dispatchers.IO) {
        val entries = folders.first()
        AppLogger.info("SCAN", "Starting library scan for " + entries.size + " folder(s)")

        entries.forEach { entry ->
            runCatching { scan(entry.uri) }.getOrElse {
                if (it is SecurityException) error("Access to the selected folder was denied.")
                AppLogger.error("SCAN", "Folder scan failed: " + entry.uri, it)
            }
        }

        AppLogger.info("SCAN", "Library scan completed")
    }

    private suspend fun scan(uri: String) {
        val root = runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(uri))
        }.getOrNull() ?: return

        val out = mutableListOf<Track>()

        suspend fun walk(directory: DocumentFile) {
            val children = runCatching { directory.listFiles().toList() }.getOrElse {
                if (it is SecurityException) throw it
                emptyList()
            }
            for (file in children) {
                if (file.isDirectory) {
                    runCatching { walk(file) }.getOrElse {
                        if (it is SecurityException) throw it
                    }
                } else if (registry.accepts(file.name.orEmpty())) {
                    out += Track(
                        uri = file.uri.toString(),
                        title = file.name?.substringBeforeLast('.')?.ifBlank { "Unknown track" } ?: "Unknown track",
                        mime = runCatching { context.contentResolver.getType(file.uri) }.getOrNull().orEmpty(),
                        size = runCatching { file.length() }.getOrDefault(0L)
                    )
                }
            }
        }

        walk(root)
        if (out.isNotEmpty()) db.tracks().upsertAll(out)
    }

    suspend fun favorite(uri: String, v: Boolean) = db.tracks().favorite(uri, v)
    suspend fun played(uri: String) = db.tracks().played(uri, System.currentTimeMillis())
}