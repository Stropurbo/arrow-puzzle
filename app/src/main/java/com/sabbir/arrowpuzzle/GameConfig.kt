package com.sabbir.arrowpuzzle

import androidx.compose.ui.graphics.Color

object GameConfig {
    const val GAME_NAME = "Tap Arrows"
    const val MAX_LEVELS = 100
    const val INITIAL_HEARTS = 3
    const val INVALID_ANIMATION_MS = 280
    const val SPLASH_MS = 1400L
    const val GRID_OVERLAY_DEFAULT = false
    const val EXIT_GLOW_ALPHA_MAX = 0.55f
    const val SELECTION_HIGHLIGHT_ALPHA = 0.28f
    const val CONFETTI_COUNT = 90
    const val ADS_ENABLED = false
    const val OFFLINE_PLAY_ENABLED = true
    const val DEFAULT_SOUND = true
    const val DEFAULT_MUSIC = true
    const val DEFAULT_HAPTICS = true
    // Arrow geometry as a fraction of one board cell, tuned to the reference art.
    const val ARROW_STROKE_FRACTION = 0.135f
    const val ARROW_STROKE_FRACTION_HC = 0.16f
    const val ARROW_HEAD_LENGTH_FRACTION = 0.34f
    const val ARROW_HEAD_HALF_WIDTH_FRACTION = 0.24f
    const val ARROW_CORNER_FRACTION = 0.17f
    const val DOT_RADIUS_FRACTION = 0.05f
    // Zoom / pan limits for dense boards.
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f
    // Haptics + audio tuning (all offline, local).
    const val HAPTIC_THROTTLE_MS = 120L
    const val SFX_VOLUME = 0.8f
    const val MUSIC_VOLUME = 0.35f
    // Per-level hint allowance shown as the badge count on the hint button.
    const val HINTS_PER_LEVEL = 2
    /** Fast, crisp exit-slide duration: snappy feel matching commercial reference puzzle game. */
    fun exitDurationMs(cells: Int, rows: Int, cols: Int): Int =
        (170 + cells * 8 + (rows + cols) * 2).coerceIn(210, 310)
    // Stars reward a clean solve: no wrong taps and no hints for three.
    fun starsFor(errors: Int, hintsUsed: Int): Int {
        val penalty = errors.coerceAtLeast(0) + hintsUsed.coerceAtLeast(0)
        return when {
            penalty == 0 -> 3
            penalty <= 2 -> 2
            else -> 1
        }
    }
    val LightBackground = Color(0xFFF6F7FB)
    val LightSurface = Color.White
    val DarkBackground = Color(0xFF141923)
    val DarkSurface = Color(0xFF202838)
    val LightArrow = Color(0xFF000000)
    val DarkArrow = Color(0xFFE5ECFC)
    val Accent = Color(0xFF5468EC)
    val HintBlue = Color(0xFF2563EB)
    val HintGlow = Color(0xFFBFDBFE)
    val Success = Color(0xFF199D77)
    val Danger = Color(0xFFE25555)
}

