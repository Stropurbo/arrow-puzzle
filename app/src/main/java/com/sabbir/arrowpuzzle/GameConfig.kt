package com.sabbir.arrowpuzzle

import androidx.compose.ui.graphics.Color

object GameConfig {
    const val GAME_NAME = "Tap Arrows"
    const val MAX_LEVELS = 101
    const val INITIAL_HEARTS = 3
    const val INVALID_ANIMATION_MS = 280
    const val SPLASH_MS = 1400L
    // Video-style: the dot grid is always visible (faint, in empty cells),
    // there is no grid toggle in the bottom dock any more.
    const val GRID_OVERLAY_DEFAULT = true
    const val EXIT_GLOW_ALPHA_MAX = 0.55f
    const val SELECTION_HIGHLIGHT_ALPHA = 0.28f
    const val CONFETTI_COUNT = 90
    const val ADS_ENABLED = false
    const val OFFLINE_PLAY_ENABLED = true
    const val DEFAULT_SOUND = true
    const val DEFAULT_MUSIC = true
    const val DEFAULT_HAPTICS = true
    // Arrow geometry as a fraction of one board cell, tuned to the reference art.
    // Video-style: thinner lines than the original black strokes.
    const val ARROW_STROKE_FRACTION = 0.11f
    const val ARROW_STROKE_FRACTION_HC = 0.14f
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
    /** Tapped-arrow colour while it slides out: the reference video's bright blue. */
    val ExitArrow = Color(0xFF3FA7FF)
    /**
     * Fade only in the last 5% of the slide, as a cleanup once the arrow is
     * already off the canvas. The video does not fade on the board at all —
     * the arrow simply slides off the screen edge — so any earlier fade
     * makes it look like it is dissolving instead of flying out.
     */
    const val EXIT_FADE_START = 0.95f
    /**
     * Measured from the reference gameplay video (30 fps frames): a tap at
     * ~0.6 s is almost gone by ~1.0 s, so a typical exit is ~350-400 ms at
     * steady speed. Longer arrows take a little longer, so speed along the
     * path stays roughly constant; the ViewModel waits exactly this long
     * before removing the piece, so the two stay in sync. The earlier
     * 420-680 ms range was too slow and felt sluggish, not video-like.
     */
    fun exitDurationMs(cells: Int, rows: Int, cols: Int): Int =
        (260 + cells * 14 + (rows + cols)).coerceIn(300, 420)
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
    // Video-style: dark navy, not pure black, in light mode.
    val LightArrow = Color(0xFF0B1F4A)
    val DarkArrow = Color(0xFFE5ECFC)
    val Accent = Color(0xFF5468EC)
    val HintBlue = Color(0xFF2563EB)
    val HintGlow = Color(0xFFBFDBFE)
    val Success = Color(0xFF199D77)
    val Danger = Color(0xFFE25555)
}

