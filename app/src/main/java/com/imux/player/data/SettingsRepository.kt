package com.imux.player.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

enum class AppearanceMode { System, Light, Dark, Amoled }
enum class AnimationMode { Full, Reduced, Off }
enum class DensityMode { Comfortable, Compact }

data class PlayerSettings(
    val shuffleDefault: Boolean = false,
    val repeatDefault: String = "OFF",
    val resumePlayback: Boolean = true,
    val autoplay: Boolean = true,
    val skipSilence: Boolean = false,
    val playbackSpeed: Float = 1f,
    val rewindSeconds: Int = 10,
    val forwardSeconds: Int = 30,
    val appearance: AppearanceMode = AppearanceMode.System,
    val dynamicColor: Boolean = true,
    val expressive: Boolean = true,
    val animation: AnimationMode = AnimationMode.Full,
    val artworkAnimations: Boolean = true,
    val density: DensityMode = DensityMode.Comfortable,
    val showMiniPlayer: Boolean = true,
    val showPlaybackProgress: Boolean = true,
    val automaticScanning: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private val formatsKey = stringSetPreferencesKey("formats")
    private val onboardingKey = booleanPreferencesKey("onboarding")
    private val shuffleKey = booleanPreferencesKey("shuffle_default")
    private val repeatKey = stringPreferencesKey("repeat_default")
    private val resumeKey = booleanPreferencesKey("resume_playback")
    private val autoplayKey = booleanPreferencesKey("autoplay")
    private val skipSilenceKey = booleanPreferencesKey("skip_silence")
    private val speedKey = floatPreferencesKey("playback_speed")
    private val rewindKey = intPreferencesKey("rewind_seconds")
    private val forwardKey = intPreferencesKey("forward_seconds")
    private val appearanceKey = stringPreferencesKey("appearance")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val expressiveKey = booleanPreferencesKey("expressive")
    private val animationKey = stringPreferencesKey("animation")
    private val artworkAnimationKey = booleanPreferencesKey("artwork_animations")
    private val densityKey = stringPreferencesKey("density")
    private val miniPlayerKey = booleanPreferencesKey("mini_player")
    private val progressKey = booleanPreferencesKey("playback_progress")
    private val automaticScanKey = booleanPreferencesKey("automatic_scan")
    private val defaults = setOf("mp3","m4a","mp4","aac","flac","wav","ogg","oga","opus","amr","3gp","3gpp")

    val formats: Flow<Set<String>> = context.store.data.map { it[formatsKey] ?: defaults }
    val onboarding: Flow<Boolean> = context.store.data.map { it[onboardingKey] ?: false }
    val settings: Flow<PlayerSettings> = context.store.data.map { p ->
        PlayerSettings(
            shuffleDefault = p[shuffleKey] ?: false,
            repeatDefault = p[repeatKey] ?: "OFF",
            resumePlayback = p[resumeKey] ?: true,
            autoplay = p[autoplayKey] ?: true,
            skipSilence = p[skipSilenceKey] ?: false,
            playbackSpeed = p[speedKey] ?: 1f,
            rewindSeconds = p[rewindKey] ?: 10,
            forwardSeconds = p[forwardKey] ?: 30,
            appearance = runCatching { AppearanceMode.valueOf(p[appearanceKey] ?: "System") }.getOrDefault(AppearanceMode.System),
            dynamicColor = p[dynamicColorKey] ?: true,
            expressive = p[expressiveKey] ?: true,
            animation = runCatching { AnimationMode.valueOf(p[animationKey] ?: "Full") }.getOrDefault(AnimationMode.Full),
            artworkAnimations = p[artworkAnimationKey] ?: true,
            density = runCatching { DensityMode.valueOf(p[densityKey] ?: "Comfortable") }.getOrDefault(DensityMode.Comfortable),
            showMiniPlayer = p[miniPlayerKey] ?: true,
            showPlaybackProgress = p[progressKey] ?: true,
            automaticScanning = p[automaticScanKey] ?: false
        )

    suspend fun done() = context.store.edit { it[onboardingKey] = true }
    suspend fun format(extension: String, enabled: Boolean) = context.store.edit {
        it[formatsKey] = (it[formatsKey] ?: defaults).let { set -> if (enabled) set + extension else set - extension }
    }
    suspend fun setShuffleDefault(v: Boolean) = context.store.edit { it[shuffleKey] = v }
    suspend fun setRepeatDefault(v: String) = context.store.edit { it[repeatKey] = v }
    suspend fun setResume(v: Boolean) = context.store.edit { it[resumeKey] = v }
    suspend fun setAutoplay(v: Boolean) = context.store.edit { it[autoplayKey] = v }
    suspend fun setSkipSilence(v: Boolean) = context.store.edit { it[skipSilenceKey] = v }
    suspend fun setSpeed(v: Float) = context.store.edit { it[speedKey] = v }
    suspend fun setRewind(v: Int) = context.store.edit { it[rewindKey] = v }
    suspend fun setForward(v: Int) = context.store.edit { it[forwardKey] = v }
    suspend fun setAppearance(v: AppearanceMode) = context.store.edit { it[appearanceKey] = v.name }
    suspend fun setDynamicColor(v: Boolean) = context.store.edit { it[dynamicColorKey] = v }
    suspend fun setExpressive(v: Boolean) = context.store.edit { it[expressiveKey] = v }
    suspend fun setAnimation(v: AnimationMode) = context.store.edit { it[animationKey] = v.name }
    suspend fun setArtworkAnimations(v: Boolean) = context.store.edit { it[artworkAnimationKey] = v }
    suspend fun setDensity(v: DensityMode) = context.store.edit { it[densityKey] = v.name }
    suspend fun setMiniPlayer(v: Boolean) = context.store.edit { it[miniPlayerKey] = v }
    suspend fun setPlaybackProgress(v: Boolean) = context.store.edit { it[progressKey] = v }
    suspend fun setAutomaticScanning(v: Boolean) = context.store.edit { it[automaticScanKey] = v }
}
