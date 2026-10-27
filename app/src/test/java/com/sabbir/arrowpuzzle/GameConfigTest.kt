package com.sabbir.arrowpuzzle

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exit-slide timing must stay readable on a phone: not a flick, not a crawl,
 * and longer arrows must take longer than shorter ones so speed along the
 * path stays roughly constant (the reference-video glide).
 */
class GameConfigTest {

    @Test
    fun exitDurationIsInReadableRange() {
        // Measured from the reference video: ~350-400 ms for a typical exit.
        val short = GameConfig.exitDurationMs(cells = 2, rows = 8, cols = 7)
        val long = GameConfig.exitDurationMs(cells = 11, rows = 18, cols = 18)
        assertTrue("short exit should be at least 300 ms, was $short", short >= 300)
        assertTrue("long exit should be at most 420 ms, was $long", long <= 420)
        assertTrue("long exit ($long) should exceed short exit ($short)", long > short)
    }

    @Test
    fun exitDurationGrowsWithArrowLength() {
        val base = GameConfig.exitDurationMs(cells = 3, rows = 10, cols = 10)
        val longer = GameConfig.exitDurationMs(cells = 8, rows = 10, cols = 10)
        assertTrue("8-cell exit ($longer) should exceed 3-cell exit ($base)", longer > base)
    }

    @Test
    fun exitFadeStartsLate() {
        // Fading only in the last 5% of the slide, never mid-board.
        assertTrue(GameConfig.EXIT_FADE_START >= 0.95f)
        assertTrue(GameConfig.EXIT_FADE_START < 1f)
    }
}
