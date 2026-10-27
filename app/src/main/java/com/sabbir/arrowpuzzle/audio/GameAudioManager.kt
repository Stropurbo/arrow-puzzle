package com.sabbir.arrowpuzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.R

/**
 * Lifecycle-aware offline audio: SoundPool SFX + looping MediaPlayer music.
 * All assets are bundled in res/raw (self-authored deterministic WAVs).
 * Respects sound/music toggles, debounces SFX per-sound (80ms each),
 * handles audio focus, fades music in on first start, and releases cleanly.
 */
class GameAudioManager(context: Context) {
    private val app = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder().setMaxStreams(4).build()
    private val ids: Map<String, Int> = mapOf(
        "tap" to pool.load(app, R.raw.sfx_tap, 1),
        "invalid" to pool.load(app, R.raw.sfx_invalid, 1),
        "heart" to pool.load(app, R.raw.sfx_heart, 1),
        "hint" to pool.load(app, R.raw.sfx_hint, 1),
        "undo" to pool.load(app, R.raw.sfx_undo, 1),
        "win" to pool.load(app, R.raw.sfx_win, 1),
        "click" to pool.load(app, R.raw.sfx_click, 1)
    )

    /** Per-sound last-play timestamps for independent 80ms debounce. */
    private val lastPlayTimes: MutableMap<String, Long> = mutableMapOf()
    private val sfxDebouncMs = 80L

    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener { change ->
            when {
                change <= 0 -> pausePlayback()
                musicEnabled && foreground -> startPlayback()
            }
        }
        .build()

    private var music: MediaPlayer? = null
    private var hasFocus = false
    private var foreground = false
    private var musicStarted = false
    var soundEnabled = true
    var musicEnabled = true

    fun play(name: String) {
        if (!soundEnabled || !foreground) return
        val now = android.os.SystemClock.uptimeMillis()
        val lastPlay = lastPlayTimes[name] ?: 0L
        if (now - lastPlay < sfxDebouncMs) return
        lastPlayTimes[name] = now
        ids[name]?.let { pool.play(it, GameConfig.SFX_VOLUME, GameConfig.SFX_VOLUME, 1, 0, 1f) }
    }

    fun startMusic() {
        foreground = true
        if (musicEnabled) startPlayback()
    }

    fun pauseMusic() {
        foreground = false
        pausePlayback()
    }

    fun applyMusicPref(enabled: Boolean) {
        musicEnabled = enabled
        if (enabled && foreground) startPlayback() else pausePlayback()
    }

    private fun startPlayback() {
        val isFirstStart = music == null
        if (isFirstStart) {
            music = MediaPlayer.create(app, R.raw.music_loop)?.apply {
                isLooping = true
                // Start silent; we'll fade in via a background thread
                setVolume(0f, 0f)
            }
        }
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) return
        }
        try {
            if (music?.isPlaying != true) {
                music?.start()
                if (isFirstStart || !musicStarted) {
                    musicStarted = true
                    // Fade from 0 to target volume over 800ms on a background thread
                    val target = GameConfig.MUSIC_VOLUME
                    val player = music
                    Thread {
                        val steps = 40
                        val stepMs = 800L / steps
                        for (i in 1..steps) {
                            val vol = (target * i.toFloat() / steps)
                            try {
                                player?.setVolume(vol, vol)
                            } catch (_: Exception) { break }
                            try { Thread.sleep(stepMs) } catch (_: InterruptedException) { break }
                        }
                        try { player?.setVolume(target, target) } catch (_: Exception) { }
                    }.apply {
                        isDaemon = true
                        start()
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun pausePlayback() {
        try {
            if (music?.isPlaying == true) music?.pause()
        } catch (_: Exception) { }
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
    }

    fun release() {
        pausePlayback()
        try { music?.stop() } catch (_: Exception) { }
        try { music?.release() } catch (_: Exception) { }
        music = null
        musicStarted = false
        pool.release()
    }
}
