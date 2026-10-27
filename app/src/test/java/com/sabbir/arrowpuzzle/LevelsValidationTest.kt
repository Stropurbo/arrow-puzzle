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
            LevelDefinition(
                o.getInt("id"), o.getInt("rows"), o.getInt("cols"), pieces,
                Difficulty.valueOf(o.getString("difficulty")),
                o.optInt("allowedMistakes", 3),
                List(rawSolution.length()) { rawSolution.getString(it) }
            )
        }
    }

    @Test fun exactly100Levels() {
        assertEquals(100, loadLevels().size)
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
