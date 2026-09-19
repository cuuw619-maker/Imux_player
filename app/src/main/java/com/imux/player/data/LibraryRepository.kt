package com.imux.player.data
import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import com.imux.player.media.SupportedAudioFormatRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
class LibraryRepository(private val db:ImuxDatabase,private val cr:ContentResolver,private val registry:SupportedAudioFormatRegistry){val tracks=db.tracks().all();val folders=db.folders().all();suspend fun addFolder(uri:String,name:String)=withContext(Dispatchers.IO){db.folders().add(Folder(uri,name));scan(uri)};suspend fun scanAll()=withContext(Dispatchers.IO){folders.first().forEach{scan(it.uri)}};private suspend fun scan(uri:String){val root=DocumentFile.fromTreeUri(cr,android.net.Uri.parse(uri))?:return;val out=mutableListOf<Track>();fun walk(d:DocumentFile){d.listFiles().forEach{f->if(f.isDirectory)walk(f) else kotlinx.coroutines.runBlocking{if(registry.accepts(f.name.orEmpty()))out+=Track(f.uri.toString(),f.name?.substringBeforeLast('.')?.ifBlank{"Unknown track"}?:"Unknown track",mime=cr.getType(f.uri).orEmpty(),size=f.length())}}};walk(root);db.tracks().upsertAll(out)};suspend fun favorite(uri:String,v:Boolean)=db.tracks().favorite(uri,v);suspend fun played(uri:String)=db.tracks().played(uri,System.currentTimeMillis())}