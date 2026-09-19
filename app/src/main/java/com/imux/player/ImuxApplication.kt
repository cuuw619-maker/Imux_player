package com.imux.player
import android.app.Application
import androidx.room.Room
import com.imux.player.data.*
import com.imux.player.media.SupportedAudioFormatRegistry
class ImuxApplication:Application(){val db by lazy{Room.databaseBuilder(this,ImuxDatabase::class.java,"imux.db").build()};val settings by lazy{SettingsRepository(this)};val formats by lazy{SupportedAudioFormatRegistry(settings)};val library by lazy{LibraryRepository(this,db,formats)}}