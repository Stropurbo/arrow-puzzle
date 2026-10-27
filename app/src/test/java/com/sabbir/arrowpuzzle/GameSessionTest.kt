package com.sabbir.arrowpuzzle

import com.sabbir.arrowpuzzle.data.GameSession
import com.sabbir.arrowpuzzle.engine.ArrowEngine
import com.sabbir.arrowpuzzle.engine.DailyLevelFactory
import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.Direction
import com.sabbir.arrowpuzzle.model.GameState
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class GameSessionTest {

    private fun demoLevel() = LevelDefinition(
        id = 7,
        rows = 6,
        cols = 6,
        pieces = listOf(
            ArrowPiece("a", listOf(GridCell(1, 1), GridCell(1, 2), GridCell(1, 3))),
            ArrowPiece("b", listOf(GridCell(3, 4), GridCell(4, 4))),
            ArrowPiece("c", listOf(GridCell(0, 5), GridCell(0, 3), GridCell(0, 2)))
        ),
        difficulty = Difficulty.EASY,
        allowedMistakes = 3,
        solution = listOf("a", "b", "c")
    )

    @Test
    fun storySessionRoundTrips() {
        val level = demoLevel()
        var state = GameState(level)
        // Play one real move so history/moves stay consistent.
        state = ArrowEngine.tap(state, "a")
        assertNotNull(state.movingPieceId)
        state = ArrowEngine.finishMove(state)

        val session = GameSession.from(state)
        val decoded = GameSession.decode(session.encode())
        assertNotNull(decoded)
        val restored = decoded!!.toGameState()

        assertNotNull(restored)
        assertEquals(state.level.id, restored!!.level.id)
        assertEquals(state.remainingIds, restored.remainingIds)
        assertEquals(state.moves, restored.moves)
        assertEquals(state.hearts, restored.hearts)
        assertEquals(state.history, restored.history)
        assertEquals(false, restored.daily)
    }

    @Test
    fun midAnimationSnapshotRestoresThePreMoveBoard() {
        val state = ArrowEngine.tap(GameState(demoLevel()), "a")
        val session = GameSession.from(state)
        val restored = session.toGameState()

        assertNotNull(restored)
        assertNull(restored!!.movingPieceId)
        assertTrue("a" in restored.remainingIds)
        assertEquals(0, restored.moves)
        assertTrue(restored.history.isEmpty())
    }

    @Test
    fun corruptPayloadIsRejected() {
        assertNull(GameSession.decode(null))
        assertNull(GameSession.decode(""))
        assertNull(GameSession.decode("not json"))
        assertNull(GameSession.decode("""{"v":1,"levelId":1}"""))
    }

    @Test
    fun unknownRemainingPieceIsRejected() {
        val session = GameSession.from(GameState(demoLevel()))
        assertNull(session.copy(remainingIds = setOf("a", "ghost")).toGameState())
    }

    @Test
    fun mismatchedHistoryAndRemainingIsRejected() {
        val session = GameSession.from(GameState(demoLevel()))
        // "a" neither remains nor has a recorded move.
        assertNull(session.copy(remainingIds = setOf("b", "c")).toGameState())
    }

    @Test
    fun overlappingOrOutOfBoundsPiecesAreRejected() {
        val session = GameSession.from(GameState(demoLevel()))
        val overlap = session.pieces.toMutableList().apply {
            this[1] = this[1].copy(cells = listOf(GridCell(1, 1), GridCell(1, 2)))
        }
        assertNull(session.copy(pieces = overlap).toGameState())

        val outside = session.pieces.toMutableList().apply {
            this[1] = this[1].copy(cells = listOf(GridCell(3, 4), GridCell(9, 4)))
        }
        assertNull(session.copy(pieces = outside).toGameState())
    }

    @Test
    fun inconsistentCountersAreRejected() {
        val session = GameSession.from(GameState(demoLevel()))
        assertNull(session.copy(moves = 4).toGameState())
        assertNull(session.copy(hearts = 9).toGameState())
        assertNull(session.copy(hintsUsed = GameConfig.HINTS_PER_LEVEL + 1).toGameState())
        assertNull(session.copy(elapsedSeconds = -1).toGameState())
        assertNull(session.copy(remainingIds = emptySet()).toGameState())
    }

    @Test
    fun dailySessionKeepsItsDateAndBoard() {
        val date = LocalDate.of(2026, 10, 27)
        val level = DailyLevelFactory.generate(date)
        val state = GameState(level, daily = true)
        val session = GameSession.from(state, dailyEpochDay = date.toEpochDay())

        val decoded = GameSession.decode(session.encode())!!
        assertEquals(true, decoded.daily)
        assertEquals(date.toEpochDay(), decoded.dailyEpochDay)
        assertEquals(level.pieces.size, decoded.toGameState()!!.level.pieces.size)
    }

    @Test
    fun everyBundledLevelCanBeSavedAndRestored() {
        val file = listOf(
            File("app/src/main/assets/levels.json"),
            File("src/main/assets/levels.json"),
            File("../app/src/main/assets/levels.json")
        ).firstOrNull { it.exists() } ?: error("levels.json not found")

        val root = JSONArray(file.readText())
        assertEquals(GameConfig.MAX_LEVELS, root.length())
        for (index in 0 until root.length()) {
            val obj = root.getJSONObject(index)
            val rawPieces = obj.getJSONArray("pieces")
            val pieces = List(rawPieces.length()) { p ->
                val po = rawPieces.getJSONObject(p)
                val rawCells = po.getJSONArray("cells")
                ArrowPiece(
                    po.getString("id"),
                    List(rawCells.length()) { c ->
                        val pair = rawCells.getJSONArray(c)
                        GridCell(pair.getInt(0), pair.getInt(1))
                    }
                )
            }
            val rawSolution = obj.getJSONArray("solution")
            val level = LevelDefinition(
                obj.getInt("id"),
                obj.getInt("rows"),
                obj.getInt("cols"),
                pieces,
                Difficulty.valueOf(obj.getString("difficulty")),
                obj.getInt("allowedMistakes"),
                List(rawSolution.length()) { rawSolution.getString(it) }
            )
            val session = GameSession.from(GameState(level))
            val restored = GameSession.decode(session.encode())?.toGameState()
            assertNotNull("level ${level.id}", restored)
            assertEquals(level.pieces.size, restored!!.level.pieces.size)
            assertEquals(level.id, restored.level.id)
        }
    }
}
