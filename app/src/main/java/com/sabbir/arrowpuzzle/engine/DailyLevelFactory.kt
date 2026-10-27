package com.sabbir.arrowpuzzle.engine

import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Builds today's Daily Challenge locally, with no network or bundled assets.
 *
 * Uses the same reverse-construction method as `tools/gen_levels.js`: pieces are
 * placed last-removed-first and a piece is only kept when it can already slide
 * out, so reversing the placement order is always a valid solution. The RNG is
 * seeded from the calendar date, which makes the board identical for everyone
 * playing that day and stable across restarts.
 */
object DailyLevelFactory {

    private val DIRS = listOf(-1 to 0, 0 to 1, 1 to 0, 0 to -1)

    fun generate(date: LocalDate): LevelDefinition {
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val seedBase = (date.toEpochDay() * 0x9E3779B1L).toInt()
        var best: List<ArrowPiece>? = null
        var bestRows = 10
        var bestCols = 10

        for (attempt in 0 until 24) {
            val rng = Rng(seedBase + attempt * 0x85EBCA6B.toInt())
            val rows = if (weekend) rng.between(11, 13) else rng.between(10, 12)
            val cols = if (weekend) rng.between(11, 13) else rng.between(10, 12)
            val target = if (weekend) rng.between(28, 34) else rng.between(22, 28)
            val pieces = build(rng, rows, cols, target)
            if (best == null || pieces.size > best!!.size) {
                best = pieces
                bestRows = rows
                bestCols = cols
            }
            if (pieces.size >= target - 2) break
        }

        val pieces = best ?: emptyList()
        return LevelDefinition(
            id = date.toEpochDay().toInt(),
            rows = bestRows,
            cols = bestCols,
            pieces = pieces,
            difficulty = if (weekend) Difficulty.HARD else Difficulty.CHALLENGING,
            allowedMistakes = 3,
            solution = pieces.map { it.id }.reversed()
        )
    }

    private fun build(rng: Rng, rows: Int, cols: Int, target: Int): List<ArrowPiece> {
        val occupied = HashSet<Int>()
        val pieces = ArrayList<ArrowPiece>()
        var stagnant = 0

        while (pieces.size < target && stagnant < 160) {
            val piece = tryPiece(rng, rows, cols, occupied, pieces)
                ?: dart(rng, rows, cols, occupied, pieces)
            if (piece == null) {
                stagnant++
                continue
            }
            stagnant = 0
            pieces.add(piece)
            for (c in piece.cells) occupied.add(c.row * cols + c.col)
        }
        return pieces
    }

    /** Long, mostly-straight winding arrow that can already exit. */
    private fun tryPiece(
        rng: Rng,
        rows: Int,
        cols: Int,
        occupied: Set<Int>,
        placed: List<ArrowPiece>
    ): ArrowPiece? {
        for (att in 0 until 140) {
            val wantLen = if (rng.chance(0.3f)) rng.between(2, 4) else rng.between(4, 8)
            var sr: Int
            var sc: Int
            if (rng.chance(0.6f)) {
                when (rng.between(0, 3)) {
                    0 -> { sr = rng.between(0, minOf(2, rows - 1)); sc = rng.between(0, cols - 1) }
                    1 -> { sr = rng.between(maxOf(0, rows - 3), rows - 1); sc = rng.between(0, cols - 1) }
                    2 -> { sr = rng.between(0, rows - 1); sc = rng.between(0, minOf(2, cols - 1)) }
                    else -> { sr = rng.between(0, rows - 1); sc = rng.between(maxOf(0, cols - 3), cols - 1) }
                }
            } else {
                sr = rng.between(0, rows - 1)
                sc = rng.between(0, cols - 1)
            }
            if (sr * cols + sc in occupied) continue

            val cells = ArrayList<GridCell>(wantLen)
            cells.add(GridCell(sr, sc))
            val inPath = HashSet<Int>()
            inPath.add(sr * cols + sc)
            var dr = 0
            var dc = 0
            var bends = 0
            val maxBends = if (wantLen >= 6) 3 else 2

            for (step in 1 until wantLen) {
                val r = cells[cells.size - 1].row
                val c = cells[cells.size - 1].col
                val options = ArrayList<Int>()
                for ((index, dir) in DIRS.withIndex()) {
                    val (ddr, ddc) = dir
                    if (dr != 0 || dc != 0) {
                        if (ddr == -dr && ddc == -dc) continue // no U-turn
                    }
                    val nr = r + ddr
                    val nc = c + ddc
                    if (nr !in 0 until rows || nc !in 0 until cols) continue
                    val key = nr * cols + nc
                    if (key in occupied || key in inPath) continue
                    val isTurn = dr != 0 && (ddr != dr || ddc != dc)
                    if (isTurn && bends >= maxBends) continue
                    // Straight runs read like the reference art; turns are rarer.
                    repeat(if (isTurn) 1 else 6) { options.add(index) }
                }
                if (options.isEmpty()) break
                val dir = DIRS[options[rng.between(0, options.size - 1)]]
                if (dr != 0 && (dir.first != dr || dir.second != dc)) bends++
                dr = dir.first
                dc = dir.second
                val nr = r + dr
                val nc = c + dc
                cells.add(GridCell(nr, nc))
                inPath.add(nr * cols + nc)
            }

            if (cells.size < 2) continue
            val candidate = ArrowPiece("p${placed.size}", cells)
            if (ExitSimulator.canExit(candidate, placed, rows, cols)) return candidate
            val reversed = ArrowPiece(candidate.id, cells.reversed())
            if (ExitSimulator.canExit(reversed, placed, rows, cols)) return reversed
        }
        return null
    }

    /** Packing pass: short straight darts into leftover corridors. */
    private fun dart(
        rng: Rng,
        rows: Int,
        cols: Int,
        occupied: Set<Int>,
        placed: List<ArrowPiece>
    ): ArrowPiece? {
        val starts = ArrayList<Int>(rows * cols)
        for (key in 0 until rows * cols) if (key !in occupied) starts.add(key)
        for (i in starts.indices.reversed()) {
            val j = rng.between(0, i)
            val tmp = starts[i]; starts[i] = starts[j]; starts[j] = tmp
        }

        for (start in starts) {
            val r = start / cols
            val c = start % cols
            val offset = rng.between(0, 3)
            for (o in 0 until 4) {
                val (ddr, ddc) = DIRS[(o + offset) % 4]
                for (len in 4 downTo 2) {
                    val cells = ArrayList<GridCell>(len)
                    var ok = true
                    for (k in len - 1 downTo 0) {
                        val nr = r - ddr * k
                        val nc = c - ddc * k
                        if (nr !in 0 until rows || nc !in 0 until cols || nr * cols + nc in occupied) {
                            ok = false
                            break
                        }
                        cells.add(GridCell(nr, nc))
                    }
                    if (!ok) continue
                    val candidate = ArrowPiece("p${placed.size}", cells)
                    if (ExitSimulator.canExit(candidate, placed, rows, cols)) return candidate
                }
            }
        }
        return null
    }

    /** mulberry32: tiny deterministic PRNG, matching the level generator tool. */
    private class Rng(seed: Int) {
        private var a: Int = seed

        fun nextFloat(): Float {
            a += 0x6D2B79F5
            var t = (a xor (a ushr 15)) * (1 or a)
            t = (t + (t xor (t ushr 7)) * 61) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
        }

        fun between(lo: Int, hi: Int): Int {
            if (hi <= lo) return lo
            val span = hi - lo + 1
            return lo + (nextFloat() * span).toInt().coerceIn(0, span - 1)
        }

        fun chance(p: Float): Boolean = nextFloat() < p
    }
}
