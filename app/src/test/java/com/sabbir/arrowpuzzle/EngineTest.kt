package com.sabbir.arrowpuzzle

import com.sabbir.arrowpuzzle.engine.ArrowEngine
import com.sabbir.arrowpuzzle.engine.ExitSimulator
import com.sabbir.arrowpuzzle.engine.LevelSolver
import com.sabbir.arrowpuzzle.engine.LevelValidator
import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.Direction
import com.sabbir.arrowpuzzle.model.GameState
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private fun level(vararg p: ArrowPiece) =
        LevelDefinition(1, 5, 5, p.toList(), Difficulty.TUTORIAL, 3, p.map { it.id })

    @Test fun fourHeadings() {
        assertEquals(Direction.UP, ArrowPiece("u", listOf(GridCell(1, 1), GridCell(0, 1))).heading)
        assertEquals(Direction.RIGHT, ArrowPiece("r", listOf(GridCell(1, 1), GridCell(1, 2))).heading)
        assertEquals(Direction.DOWN, ArrowPiece("d", listOf(GridCell(1, 1), GridCell(2, 1))).heading)
        assertEquals(Direction.LEFT, ArrowPiece("l", listOf(GridCell(1, 1), GridCell(1, 0))).heading)
    }

    @Test fun singleCellHeading() {
        assertEquals(Direction.UP, ArrowPiece("s", listOf(GridCell(2, 2)), Direction.UP).heading)
    }

    @Test fun clearAndBlockedExits() {
        val a = ArrowPiece("a", listOf(GridCell(2, 1), GridCell(2, 2)))
        val b = ArrowPiece("b", listOf(GridCell(2, 4), GridCell(2, 3)))
        assertTrue(ExitSimulator.canExit(a, listOf(a), 5, 5))
        assertFalse(ExitSimulator.canExit(a, listOf(a, b), 5, 5))
    }

    @Test fun edgeAndCornerExits() {
        // Head already facing off-board from a corner exits immediately.
        val corner = ArrowPiece("c", listOf(GridCell(0, 1), GridCell(0, 0)), Direction.LEFT)
        assertTrue(ExitSimulator.canExit(corner, listOf(corner), 5, 5))
        // Bottom edge heading down with clear column.
        val edge = ArrowPiece("e", listOf(GridCell(3, 2), GridCell(4, 2)))
        assertTrue(ExitSimulator.canExit(edge, listOf(edge), 5, 5))
    }

    @Test fun bentPathExit() {
        // L-shaped piece heading right along a clear row.
        val bent = ArrowPiece("bent", listOf(GridCell(1, 0), GridCell(2, 0), GridCell(2, 1), GridCell(2, 2)))
        assertTrue(ExitSimulator.canExit(bent, listOf(bent), 5, 5))
    }

    @Test fun collisionDuringMotionBlocked() {
        // A exits right but B sits in its future path (not just head cell).
        val a = ArrowPiece("a", listOf(GridCell(2, 0), GridCell(2, 1)))
        val b = ArrowPiece("b", listOf(GridCell(1, 3), GridCell(2, 3), GridCell(3, 3)), Direction.DOWN)
        assertFalse(ExitSimulator.canExit(a, listOf(a, b), 5, 5))
    }

    @Test fun moveUndoAndWin() {
        val l = level(ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))))
        val moving = ArrowEngine.tap(GameState(l), "a")
        assertEquals("a", moving.movingPieceId)
        val won = ArrowEngine.finishMove(moving)
        assertTrue(won.completed)
        assertFalse(ArrowEngine.undo(won).completed)
    }

    @Test fun invalidCostsHeart() {
        val a = ArrowPiece("a", listOf(GridCell(2, 1), GridCell(2, 2)))
        val b = ArrowPiece("b", listOf(GridCell(2, 4), GridCell(2, 3)))
        val l = level(a, b)
        assertEquals(2, ArrowEngine.tap(GameState(l), "a").hearts)
    }

    @Test fun doubleTapWhileMovingIgnored() {
        val l = level(
            ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))),
            ArrowPiece("b", listOf(GridCell(4, 0), GridCell(4, 1)))
        )
        val moving = ArrowEngine.tap(GameState(l), "a")
        // Second tap while a piece is animating must be ignored.
        assertEquals(moving, ArrowEngine.tap(moving, "b"))
    }

    @Test fun invalidTapUnknownIdIgnored() {
        val l = level(ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))))
        val s = GameState(l)
        assertEquals(s, ArrowEngine.tap(s, "nope"))
    }

    @Test fun tapEmptyAndPausedIgnored() {
        val l = level(ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))))
        val paused = GameState(l).copy(paused = true)
        assertEquals(paused, ArrowEngine.tap(paused, "a"))
    }

    @Test fun undoRestoresAndClearsWin() {
        val l = level(
            ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))),
            ArrowPiece("b", listOf(GridCell(4, 0), GridCell(4, 1)))
        )
        var s = ArrowEngine.finishMove(ArrowEngine.tap(GameState(l), "a"))
        assertEquals(1, s.moves)
        s = ArrowEngine.undo(s)
        assertEquals(0, s.moves)
        assertEquals(setOf("a", "b"), s.remainingIds)
        // Undo with empty history is a no-op.
        assertEquals(s, ArrowEngine.undo(s))
    }

    @Test fun hintNeverSuggestsBlocked() {
        val a = ArrowPiece("a", listOf(GridCell(2, 1), GridCell(2, 2)))
        val b = ArrowPiece("b", listOf(GridCell(2, 4), GridCell(2, 3)))
        val l = level(a, b)
        val hinted = ArrowEngine.hint(GameState(l))
        // b heads LEFT into a, so only... verify hint is actually safe.
        val safe = LevelSolver.safePieces(l, hinted.remainingIds)
        if (hinted.hintedPieceId != null) assertTrue(hinted.hintedPieceId in safe)
    }

    @Test fun gameOverAfterThreeMistakes() {
        val a = ArrowPiece("a", listOf(GridCell(2, 1), GridCell(2, 2)))
        val b = ArrowPiece("b", listOf(GridCell(2, 4), GridCell(2, 3)))
        val l = LevelDefinition(1, 5, 5, listOf(a, b), Difficulty.TUTORIAL, 3, listOf("b", "a"))
        var s = GameState(l)
        repeat(3) { s = ArrowEngine.tap(s.copy(invalidPieceId = null), "a") }
        assertTrue(s.outOfHearts)
        assertEquals(0, s.hearts)
    }

    @Test fun validatorCatchesOverlapAndBadSolution() {
        val a = ArrowPiece("a", listOf(GridCell(0, 0), GridCell(0, 1)))
        val b = ArrowPiece("b", listOf(GridCell(0, 1), GridCell(0, 2)))
        val bad = LevelDefinition(1, 5, 5, listOf(a, b), Difficulty.TUTORIAL, 3, listOf("a"))
        assertTrue(LevelValidator.errors(bad).isNotEmpty())
        // Unknown solution id.
        val badSol = LevelDefinition(1, 5, 5, listOf(a), Difficulty.TUTORIAL, 3, listOf("zzz"))
        assertTrue(LevelValidator.errors(badSol).isNotEmpty())
    }

    @Test fun solverCompletenessOnSmallLevel() {
        val a = ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4)))
        val b = ArrowPiece("b", listOf(GridCell(4, 0), GridCell(4, 1)))
        val l = level(a, b)
        val order = LevelSolver.solve(l)
        assertTrue(order != null && order!!.size == 2)
        assertTrue(LevelValidator.errors(l).isEmpty())
    }

    @Test fun hintBudgetCappedPerLevel() {
        val l = level(ArrowPiece("a", listOf(GridCell(0, 3), GridCell(0, 4))))
        var s = GameState(l)
        repeat(com.sabbir.arrowpuzzle.GameConfig.HINTS_PER_LEVEL) { s = ArrowEngine.hint(s) }
        assertEquals(com.sabbir.arrowpuzzle.GameConfig.HINTS_PER_LEVEL, s.hintsUsed)
        val capped = ArrowEngine.hint(s)
        assertEquals(s.hintsUsed, capped.hintsUsed)
        assertEquals(s, capped)
    }

    @Test fun unsolvableDetected() {
        // Two pieces head-on into each other: neither can ever exit.
        val a = ArrowPiece("a", listOf(GridCell(2, 1), GridCell(2, 2)))
        val b = ArrowPiece("b", listOf(GridCell(2, 4), GridCell(2, 3)))
        val l = LevelDefinition(1, 5, 5, listOf(a, b), Difficulty.TUTORIAL, 3, listOf("a", "b"))
        assertNull(LevelSolver.solve(l))
    }

    @Test fun starsRewardCleanSolves() {
        // A perfect board with no hints earns all three stars.
        assertEquals(3, GameConfig.starsFor(errors = 0, hintsUsed = 0))
        // A single slip or a single hint still earns two.
        assertEquals(2, GameConfig.starsFor(errors = 1, hintsUsed = 0))
        assertEquals(2, GameConfig.starsFor(errors = 0, hintsUsed = 1))
        // Heavy help drops to one star.
        assertEquals(1, GameConfig.starsFor(errors = 2, hintsUsed = 1))
        assertEquals(1, GameConfig.starsFor(errors = 3, hintsUsed = 2))
        // Negative values can never inflate a rating.
        assertEquals(3, GameConfig.starsFor(errors = -4, hintsUsed = -1))
    }
}
