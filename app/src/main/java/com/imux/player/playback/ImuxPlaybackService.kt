package com.imux.player.playback
import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.imux.player.MainActivity
class ImuxPlaybackService:MediaSessionService(){lateinit var player:ExoPlayer;lateinit var session:MediaSession
override fun onCreate(){super.onCreate();player=ExoPlayer.Builder(this).setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),true).setHandleAudioBecomingNoisy(true).build();val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE);session=MediaSession.Builder(this,player).setSessionActivity(pi).build()}
override fun onGetSession(i:MediaSession.ControllerInfo)=session
override fun onDestroy(){session.release();player.release();super.onDestroy()}}