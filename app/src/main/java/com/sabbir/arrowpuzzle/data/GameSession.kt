package com.sabbir.arrowpuzzle.data

import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.Direction
import com.sabbir.arrowpuzzle.model.GameState
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import com.sabbir.arrowpuzzle.model.RemovedMove
import org.json.JSONArray
import org.json.JSONObject

/**
 * A compact, validated snapshot of an in-progress board.
 *
 * The session is intentionally independent of the bundled level list: the whole
 * board is stored, so a save can still be resumed after the app is killed, and a
 * daily board can be restored without regenerating it. Every field is checked in
 * [toGameState]; a corrupt or outdated save is dropped instead of crashing.
 */
data class GameSession(
    val levelId: Int,
    val daily: Boolean,
    /** Epoch day of the daily puzzle, 0 for story levels. */
    val dailyEpochDay: Long,
    val difficulty: Difficulty,
    val allowedMistakes: Int,
    val rows: Int,
    val cols: Int,
    val pieces: List<ArrowPiece>,
    val solution: List<String>,
    val remainingIds: Set<String>,
    val moves: Int,
    val errors: Int,
    val hearts: Int,
    val elapsedSeconds: Int,
    val hintsUsed: Int,
    val history: List<String>
) {
    /** Rebuilds a playable state, or null when this save cannot be trusted. */
    fun toGameState(): GameState? {
        if (rows !in 2..64 || cols !in 2..64) return null
        if (pieces.isEmpty() || pieces.size > 512) return null
        if (allowedMistakes <= 0 || hearts !in 0..allowedMistakes) return null
        if (moves < 0 || errors < 0 || elapsedSeconds < 0) return null
        if (hintsUsed !in 0..GameConfig.HINTS_PER_LEVEL) return null
        if (!daily && levelId !in 1..GameConfig.MAX_LEVELS) return null

        val ids = pieces.mapTo(LinkedHashSet()) { it.id }
        if (ids.size != pieces.size) return null
        if (remainingIds.isEmpty() || !ids.containsAll(remainingIds)) return null
        if (history.any { it !in ids } || history.distinct().size != history.size) return null
        if (history.any { it in remainingIds }) return null
        if ((remainingIds + history).size != ids.size) return null
        if (moves != history.size) return null

        val occupied = HashSet<GridCell>()
        for (piece in pieces) {
            if (piece.cells.isEmpty()) return null
            if (piece.cells.distinct().size != piece.cells.size) return null
            for (cell in piece.cells) {
                if (cell.row !in 0 until rows || cell.col !in 0 until cols) return null
                if (!occupied.add(cell)) return null
            }
            // Heading is derived lazily; validate it now so the renderer can
            // never meet a piece that throws mid-animation.
            runCatching { piece.heading }.getOrNull() ?: return null
        }

        val level = LevelDefinition(
            id = levelId,
            rows = rows,
            cols = cols,
            pieces = pieces,
            difficulty = difficulty,
            allowedMistakes = allowedMistakes,
            solution = solution.ifEmpty { pieces.map { it.id }.reversed() }
        )
        return GameState(
            level = level,
            remainingIds = remainingIds.toCollection(LinkedHashSet()),
            moves = moves,
            errors = errors,
            hearts = hearts,
            elapsedSeconds = elapsedSeconds,
            hintsUsed = hintsUsed,
            history = history.map(::RemovedMove),
            daily = daily
        )
    }

    fun encode(): String {
        val root = JSONObject()
        root.put("v", VERSION)
        root.put("levelId", levelId)
        root.put("daily", daily)
        root.put("dailyEpochDay", dailyEpochDay)
        root.put("difficulty", difficulty.name)
        root.put("allowedMistakes", allowedMistakes)
        root.put("rows", rows)
        root.put("cols", cols)
        root.put("pieces", JSONArray().apply {
            pieces.forEach { piece ->
                put(JSONObject().apply {
                    put("id", piece.id)
                    put("cells", JSONArray().apply {
                        piece.cells.forEach { cell ->
                            put(JSONArray().put(cell.row).put(cell.col))
                        }
                    })
                    piece.singleCellHeading?.let { put("heading", it.name) }
                })
            }
        })
        root.put("solution", JSONArray(solution))
        root.put("remainingIds", JSONArray(remainingIds.toList()))
        root.put("moves", moves)
        root.put("errors", errors)
        root.put("hearts", hearts)
        root.put("elapsedSeconds", elapsedSeconds)
        root.put("hintsUsed", hintsUsed)
        root.put("history", JSONArray(history))
        return root.toString()
    }

    companion object {
        private const val VERSION = 1

        /** Snapshot of a state. A move mid-animation is saved as not-yet-played. */
        fun from(state: GameState, dailyEpochDay: Long = 0L): GameSession {
            val level = state.level
            return GameSession(
                levelId = level.id,
                daily = state.daily,
                dailyEpochDay = dailyEpochDay,
                difficulty = level.difficulty,
                allowedMistakes = level.allowedMistakes,
                rows = level.rows,
                cols = level.cols,
                pieces = level.pieces,
                solution = level.solution,
                remainingIds = state.remainingIds.toSet(),
                moves = state.moves,
                errors = state.errors,
                hearts = state.hearts,
                elapsedSeconds = state.elapsedSeconds,
                hintsUsed = state.hintsUsed,
                history = state.history.map { it.pieceId }
            )
        }

        /** Decodes a saved session. Returns null for corrupt or older payloads. */
        fun decode(raw: String?): GameSession? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val root = JSONObject(raw)
                if (root.optInt("v", 0) != VERSION) return null

                val rawPieces = root.getJSONArray("pieces")
                val pieces = ArrayList<ArrowPiece>(rawPieces.length())
                for (i in 0 until rawPieces.length()) {
                    val obj = rawPieces.getJSONObject(i)
                    val rawCells = obj.getJSONArray("cells")
                    val cells = ArrayList<GridCell>(rawCells.length())
                    for (c in 0 until rawCells.length()) {
                        val pair = rawCells.getJSONArray(c)
                        cells.add(GridCell(pair.getInt(0), pair.getInt(1)))
                    }
                    val heading = if (obj.has("heading")) {
                        Direction.valueOf(obj.getString("heading"))
                    } else null
                    pieces.add(ArrowPiece(obj.getString("id"), cells, heading))
                }

                GameSession(
                    levelId = root.getInt("levelId"),
                    daily = root.getBoolean("daily"),
                    dailyEpochDay = root.optLong("dailyEpochDay", 0L),
                    difficulty = Difficulty.valueOf(root.getString("difficulty")),
                    allowedMistakes = root.getInt("allowedMistakes"),
                    rows = root.getInt("rows"),
                    cols = root.getInt("cols"),
                    pieces = pieces,
                    solution = root.getJSONArray("solution").toStringList(),
                    remainingIds = root.getJSONArray("remainingIds").toStringList().toSet(),
                    moves = root.getInt("moves"),
                    errors = root.getInt("errors"),
                    hearts = root.getInt("hearts"),
                    elapsedSeconds = root.getInt("elapsedSeconds"),
                    hintsUsed = root.getInt("hintsUsed"),
                    history = root.getJSONArray("history").toStringList()
                )
            }.getOrNull()
        }

        private fun JSONArray.toStringList(): List<String> =
            List(length()) { getString(it) }
    }
}
