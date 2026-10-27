package com.sabbir.arrowpuzzle

import com.sabbir.arrowpuzzle.engine.LevelSolver
import com.sabbir.arrowpuzzle.engine.LevelValidator
import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.Direction
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Loads every bundled level and runs the independent validator + solver. */
class LevelsValidationTest {
    private fun loadLevels(): List<LevelDefinition> {
        val candidates = listOf(
            File("app/src/main/assets/levels.json"),
            File("src/main/assets/levels.json"),
            File("../app/src/main/assets/levels.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("levels.json not found from ${System.getProperty("user.dir")}")
        val root = JSONArray(file.readText())
        return List(root.length()) { i ->
            val o = root.getJSONObject(i)
            val rawPieces = o.getJSONArray("pieces")
            val pieces = List(rawPieces.length()) { p ->
                val po = rawPieces.getJSONObject(p)
                val rawCells = po.getJSONArray("cells")
                val cells = List(rawCells.length()) { c ->
                    val pair = rawCells.getJSONArray(c)
                    GridCell(pair.getInt(0), pair.getInt(1))
                }
                val heading = if (po.has("heading")) runCatching {
                    Direction.valueOf(po.getString("heading"))
                }.getOrNull() else null
                ArrowPiece(po.getString("id"), cells, heading)
            }
            val rawSolution = o.getJSONArray("solution")
            val shape = if (o.has("shape")) {
                val rawShape = o.getJSONArray("shape")
                List(rawShape.length()) { s -> val pair = rawShape.getJSONArray(s); GridCell(pair.getInt(0), pair.getInt(1)) }.toSet()
            } else null
            LevelDefinition(
                o.getInt("id"), o.getInt("rows"), o.getInt("cols"), pieces,
                Difficulty.valueOf(o.getString("difficulty")),
                o.optInt("allowedMistakes", 3),
                List(rawSolution.length()) { rawSolution.getString(it) },
                shape,
                if (o.has("shapeName")) o.getString("shapeName") else null
            )
        }
    }

    @Test fun exactly101Levels() {
        assertEquals(101, loadLevels().size)
    }

    @Test fun shapedLevelsAreRealShapes() {
        val shaped = loadLevels().filter { it.isShaped }
        assertTrue("expected at least one shaped level", shaped.isNotEmpty())
        shaped.forEach { level ->
            assertTrue("shaped level ${level.id} must not cover the full rectangle",
                level.shape!!.size < level.rows * level.cols)
            assertTrue("shaped level ${level.id} pieces must stay inside the shape",
                level.pieces.all { piece -> piece.cells.all { it in level.playableCells } })
            // Heart level 101 is left-right symmetric.
            if (level.id == 101) {
                assertEquals("Heart", level.shapeName)
                level.shape!!.forEach { cell ->
                    assertTrue("heart must be symmetric at $cell",
                        GridCell(cell.row, level.cols - 1 - cell.col) in level.shape!!)
                }
            }
        }
    }

    @Test fun allLevelsPassValidatorAndSolver() {
        val levels = loadLevels()
        val failures = mutableListOf<String>()
        levels.forEach { level ->
            val errs = LevelValidator.errors(level)
            if (errs.isNotEmpty()) failures += "L${level.id}: ${errs.take(3)}"
            if (LevelSolver.solve(level) == null) failures += "L${level.id}: greedy solver found no order"
        }
        assertTrue("Failures:\n${failures.take(10).joinToString("\n")}", failures.isEmpty())
    }

    @Test fun difficultyTiersPresent() {
        val diffs = loadLevels().map { it.difficulty }.toSet()
        assertTrue(diffs.containsAll(listOf(Difficulty.TUTORIAL, Difficulty.EASY, Difficulty.EXPERT)))
    }
}
