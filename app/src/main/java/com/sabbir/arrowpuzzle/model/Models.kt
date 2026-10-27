package com.sabbir.arrowpuzzle.model

data class GridCell(val row: Int, val col: Int) {
    operator fun plus(direction: Direction) = GridCell(row + direction.dr, col + direction.dc)
}
enum class Direction(val dr: Int, val dc: Int) { UP(-1,0), RIGHT(0,1), DOWN(1,0), LEFT(0,-1) }
enum class Difficulty { TUTORIAL, EASY, MEDIUM, CHALLENGING, HARD, EXPERT }
data class ArrowPiece(val id: String, val cells: List<GridCell>, val singleCellHeading: Direction? = null) {
    val heading: Direction get() {
        singleCellHeading?.let { return it }
        require(cells.size >= 2)
        val a = cells[cells.lastIndex - 1]; val b = cells.last()
        return Direction.entries.first { it.dr == b.row - a.row && it.dc == b.col - a.col }
    }
}
data class LevelDefinition(val id: Int, val rows: Int, val cols: Int, val pieces: List<ArrowPiece>, val difficulty: Difficulty, val allowedMistakes: Int = 3, val solution: List<String>)
data class RemovedMove(val pieceId: String)
data class GameState(
    val level: LevelDefinition,
    val remainingIds: Set<String> = level.pieces.mapTo(linkedSetOf()) { it.id },
    val moves: Int = 0, val errors: Int = 0, val hearts: Int = level.allowedMistakes,
    val elapsedSeconds: Int = 0, val hintsUsed: Int = 0,
    val history: List<RemovedMove> = emptyList(),
    val movingPieceId: String? = null,
    val undoingPieceId: String? = null,
    val invalidPieceId: String? = null,
    val hintedPieceId: String? = null,
    val paused: Boolean = false, val completed: Boolean = false, val outOfHearts: Boolean = false,
    /** True when this board is today's Daily Challenge instead of a story level. */
    val daily: Boolean = false,
)

