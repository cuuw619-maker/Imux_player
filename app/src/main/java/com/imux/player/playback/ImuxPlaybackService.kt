package com.imux.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.imux.player.MainActivity

@OptIn(UnstableApi::class)
class ImuxPlaybackService : MediaSessionService() {
    lateinit var player: ExoPlayer
    lateinit var session: MediaSession

    private val shuffleCommand = SessionCommand(
        "com.imux.player.SHUFFLE",
        Bundle.EMPTY
    )
    private val repeatCommand = SessionCommand(
        "com.imux.player.REPEAT",
        Bundle.EMPTY
    )
    private val nextCommand = SessionCommand(
        "com.imux.player.NEXT",
        Bundle.EMPTY
    )
    private val previousCommand = SessionCommand(
        "com.imux.player.PREVIOUS",
        Bundle.EMPTY
    )

    private val callback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(shuffleCommand)
                .add(repeatCommand)
                .add(nextCommand)
                .add(previousCommand)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ) = if (customCommand == shuffleCommand) {
            PlaybackCommandBus.send(PlaybackCommandBus.SHUFFLE)
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } else if (customCommand == repeatCommand) {
            PlaybackCommandBus.send(PlaybackCommandBus.REPEAT)
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } else if (customCommand == nextCommand) {
            PlaybackCommandBus.send(PlaybackCommandBus.NEXT)
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } else if (customCommand == previousCommand) {
            PlaybackCommandBus.send(PlaybackCommandBus.PREVIOUS)
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } else {
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val buttons = listOf(
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setSessionCommand(previousCommand)
                .setDisplayName("Previous")
                .setSlots(CommandButton.SLOT_BACK)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                .setSessionCommand(shuffleCommand)
                .setDisplayName("Shuffle")
                .setSlots(CommandButton.SLOT_BACK_SECONDARY)
                .build(),
            CommandButton.Builder(CommandButton.ICON_PLAY)
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .setDisplayName("Play / pause")
                .setSlots(CommandButton.SLOT_CENTRAL)
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setSessionCommand(nextCommand)
                .setDisplayName("Next")
                .setSlots(CommandButton.SLOT_FORWARD)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                .setSessionCommand(repeatCommand)
                .setDisplayName("Repeat")
                .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
                .build()
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(callback)
            .setMediaButtonPreferences(buttons)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }
}
