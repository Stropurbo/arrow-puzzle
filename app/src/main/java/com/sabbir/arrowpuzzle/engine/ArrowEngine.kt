package com.sabbir.arrowpuzzle.engine

import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.model.*
import kotlin.math.abs

// Shaped boards need no special exit rule: cells outside the shape but inside
// the bounding rectangle are simply empty space, so an arrow may slide through
// them to leave the board. Only piece cells block an exit, exactly as here.
object ExitSimulator {
    fun canExit(piece: ArrowPiece, others: Collection<ArrowPiece>, rows: Int, cols: Int): Boolean {
        val occupied = others.filter { it.id != piece.id }.flatMapTo(hashSetOf()) { it.cells }
        var pose = piece.cells
        repeat(rows + cols + piece.cells.size + 2) {
            if (pose.all { it.row !in 0 until rows || it.col !in 0 until cols }) return true
            val next = pose.drop(1) + (pose.last() + piece.heading)
            val inBoard = next.filter { it.row in 0 until rows && it.col in 0 until cols }
            if (inBoard.size != inBoard.distinct().size || inBoard.any { it in occupied }) return false
            pose = next
        }
        return false
    }
}
object LevelSolver {
    fun safePieces(level: LevelDefinition, remaining: Set<String>): List<String> {
        val pieces = level.pieces.filter { it.id in remaining }
        return pieces.filter { ExitSimulator.canExit(it, pieces, level.rows, level.cols) }.map { it.id }
    }
    fun solve(level: LevelDefinition): List<String>? {
        val remaining = level.pieces.mapTo(linkedSetOf()) { it.id }; val result = mutableListOf<String>()
        while (remaining.isNotEmpty()) { val safe = safePieces(level, remaining).firstOrNull() ?: return null; result += safe; remaining -= safe }
        return result
    }
}
object LevelValidator {
    fun errors(level: LevelDefinition): List<String> = buildList {
        if (level.rows < 2 || level.cols < 2) add("invalid board")
        if (level.pieces.isEmpty()) add("empty level")
        val ids = hashSetOf<String>(); val occupied = hashSetOf<GridCell>()
        val playable = level.playableCells
        level.pieces.forEach { piece ->
            if (!ids.add(piece.id)) add("duplicate id ${piece.id}")
            if (piece.cells.isEmpty()) add("empty piece ${piece.id}")
            piece.cells.forEach { cell ->
                if (cell.row !in 0 until level.rows || cell.col !in 0 until level.cols) add("out of bounds ${piece.id}")
                if (cell !in playable) add("outside shape ${piece.id}")
                if (!occupied.add(cell)) add("overlap at $cell")
            }
            if (piece.cells.distinct().size != piece.cells.size) add("self overlap ${piece.id}")
            piece.cells.zipWithNext().forEach { (a,b) -> if (abs(a.row-b.row)+abs(a.col-b.col) != 1) add("non-orthogonal ${piece.id}") }
            runCatching { piece.heading }.onFailure { add("bad heading ${piece.id}") }
        }
        val remaining = level.pieces.mapTo(linkedSetOf()) { it.id }
        level.solution.forEach { id -> if (id !in remaining) add("invalid solution id $id") else if (id !in LevelSolver.safePieces(level, remaining)) add("blocked solution step $id") else remaining -= id }
        if (remaining.isNotEmpty()) add("solution incomplete")
        if (LevelSolver.solve(level) == null) add("unsolvable")
    }
}
object ArrowEngine {
    fun tap(state: GameState, pieceId: String): GameState {
        if (state.paused || state.completed || state.outOfHearts || state.movingPieceId != null || state.undoingPieceId != null || pieceId !in state.remainingIds) return state
        return if (pieceId in LevelSolver.safePieces(state.level, state.remainingIds)) state.copy(movingPieceId = pieceId, hintedPieceId = null, undoingPieceId = null)
        else state.copy(errors = state.errors + 1, hearts = (state.hearts - 1).coerceAtLeast(0), invalidPieceId = pieceId, outOfHearts = state.hearts <= 1, undoingPieceId = null)
    }
    fun finishMove(state: GameState): GameState { val id = state.movingPieceId ?: return state; val remaining = state.remainingIds - id; return state.copy(remainingIds = remaining, moves = state.moves + 1, history = state.history + RemovedMove(id), movingPieceId = null, completed = remaining.isEmpty()) }
    fun clearInvalid(state: GameState) = state.copy(invalidPieceId = null)
    fun clearUndoing(state: GameState) = state.copy(undoingPieceId = null)
    fun undo(state: GameState): GameState {
        if (state.movingPieceId != null || state.undoingPieceId != null || state.history.isEmpty()) return state
        val last = state.history.lastOrNull() ?: return state
        if (last.pieceId in state.remainingIds || state.level.pieces.none { it.id == last.pieceId }) return state
        return state.copy(
            remainingIds = state.remainingIds + last.pieceId,
            moves = (state.moves - 1).coerceAtLeast(0),
            history = state.history.dropLast(1),
            undoingPieceId = last.pieceId,
            completed = false,
            hintedPieceId = null
        )
    }
    fun hint(state: GameState): GameState {
        if (state.hintsUsed >= GameConfig.HINTS_PER_LEVEL) return state
        return state.copy(hintedPieceId = LevelSolver.safePieces(state.level, state.remainingIds).firstOrNull(), hintsUsed = state.hintsUsed + 1)
    }
}

