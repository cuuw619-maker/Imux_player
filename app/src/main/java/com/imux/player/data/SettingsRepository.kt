package com.imux.player.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
private val Context.store by preferencesDataStore("settings")
class SettingsRepository(private val c:Context){private val f=stringSetPreferencesKey("formats");private val o=booleanPreferencesKey("onboarding");private val defaults=setOf("mp3","m4a","mp4","aac","flac","wav","ogg","oga","opus","amr","3gp","3gpp");val formats=c.store.data.map{it[f]?:defaults};val onboarding=c.store.data.map{it[o]?:false};suspend fun done()=c.store.edit{it[o]=true};suspend fun format(x:String,v:Boolean)=c.store.edit{it[f]=(it[f]?:defaults).let{s->if(v)s+x else s-x}}}