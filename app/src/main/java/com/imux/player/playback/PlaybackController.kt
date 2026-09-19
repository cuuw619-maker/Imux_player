package com.imux.player.playback
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.imux.player.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
class PlaybackController(context:Context){
 private val future:ListenableFuture<MediaController> =MediaController.Builder(context,SessionToken(context,ComponentName(context,ImuxPlaybackService::class.java))).buildAsync()
 val current=MutableStateFlow<Track?>(null)
 fun play(t:Track){future.get().apply{setMediaItem(MediaItem.Builder().setUri(Uri.parse(t.uri)).setMediaId(t.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build()).build());prepare();play()};current.value=t}
 fun toggle(){future.get().let{if(it.isPlaying)it.pause()else it.play()}}
 fun release(){MediaController.releaseFuture(future)}
}