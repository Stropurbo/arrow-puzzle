package com.sabbir.arrowpuzzle.data

import android.content.Context
import com.sabbir.arrowpuzzle.model.*
import org.json.JSONArray

class LevelRepository(context: Context) {
    val levels: List<LevelDefinition> = context.assets.open("levels.json").bufferedReader().use { parse(JSONArray(it.readText())) }
    private fun parse(root: JSONArray) = List(root.length()) { index ->
        val o = root.getJSONObject(index); val rawPieces = o.getJSONArray("pieces")
        val pieces = List(rawPieces.length()) { p ->
            val po = rawPieces.getJSONObject(p); val rawCells = po.getJSONArray("cells")
            val cells = List(rawCells.length()) { c -> val pair = rawCells.getJSONArray(c); GridCell(pair.getInt(0), pair.getInt(1)) }
            val heading = if (po.has("heading")) runCatching { Direction.valueOf(po.getString("heading")) }.getOrNull() else null
            ArrowPiece(po.getString("id"), cells, heading)
        }
        val rawSolution = o.getJSONArray("solution")
        LevelDefinition(
            o.getInt("id"),
            o.getInt("rows"),
            o.getInt("cols"),
            pieces,
            Difficulty.valueOf(o.getString("difficulty")),
            o.optInt("allowedMistakes", 3),
            List(rawSolution.length()) { rawSolution.getString(it) }
        )
    }
}

