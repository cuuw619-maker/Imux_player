package com.imux.player.data

import android.content.Context
import android.net.Uri
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

    suspend fun addFolder(uri: String, name: String) = withContext(Dispatchers.IO) {
        val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri)) }.getOrNull()
            ?: error("Android did not provide access to the selected folder.")

        runCatching { root.listFiles() }.getOrElse {
            error("The selected folder is not readable. Please choose the folder again.")
        }

        db.folders().add(Folder(uri, name.ifBlank { "Music" }))
        runCatching { scan(uri) }.getOrElse {
            if (it is SecurityException) error("Access to the selected folder was denied.")
        }
    }

    suspend fun scanAll() = withContext(Dispatchers.IO) {
        folders.first().forEach { entry ->
            runCatching { scan(entry.uri) }.getOrElse {
                if (it is SecurityException) error("Access to the selected folder was denied.")
            }
        }
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