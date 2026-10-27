package com.sabbir.arrowpuzzle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Small hand-drawn icon set for the HUD and navigation.
 *
 * Every glyph here is a Compose Canvas path, so the UI stays crisp at any
 * density and never depends on emoji fonts being present on the device. Shapes
 * use the same flat, rounded style as the board arrows.
 */

/** Filled or outlined heart used for the lives row. */
@Composable
fun HeartIcon(
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.90f)
            cubicTo(s * 0.06f, s * 0.60f, s * 0.02f, s * 0.34f, s * 0.21f, s * 0.18f)
            cubicTo(s * 0.38f, s * 0.04f, s * 0.50f, s * 0.20f, s * 0.50f, s * 0.30f)
            cubicTo(s * 0.50f, s * 0.20f, s * 0.62f, s * 0.04f, s * 0.79f, s * 0.18f)
            cubicTo(s * 0.98f, s * 0.34f, s * 0.94f, s * 0.60f, s * 0.50f, s * 0.90f)
            close()
        }
        if (filled) drawPath(path, color)
        else drawPath(path, color, style = Stroke(width = s * 0.13f, join = StrokeJoin.Round))
    }
}

/** Light bulb for the hint button. */
@Composable
fun HintIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.115f
        val bulb = Path().apply {
            moveTo(s * 0.33f, s * 0.58f)
            cubicTo(s * 0.17f, s * 0.44f, s * 0.22f, s * 0.17f, s * 0.50f, s * 0.17f)
            cubicTo(s * 0.78f, s * 0.17f, s * 0.83f, s * 0.44f, s * 0.67f, s * 0.58f)
        }
        drawPath(bulb, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color, Offset(s * 0.37f, s * 0.70f), Offset(s * 0.63f, s * 0.70f),
            w, StrokeCap.Round
        )
        drawLine(
            color, Offset(s * 0.42f, s * 0.83f), Offset(s * 0.58f, s * 0.83f),
            w, StrokeCap.Round
        )
        drawLine(
            color, Offset(s * 0.50f, s * 0.31f), Offset(s * 0.50f, s * 0.45f),
            w * 0.75f, StrokeCap.Round
        )
    }
}

/** 3x3 dot grid, matching the board's dot guide. */
@Composable
fun GridToggleIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val r = s * 0.085f
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(color, r, Offset(s * (0.22f + col * 0.28f), s * (0.22f + row * 0.28f)))
            }
        }
    }
}

/** Cog for the top-bar pause/settings action. */
@Composable
fun GearIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(s / 2f, s / 2f)
        val ringRadius = s * 0.225f
        val toothInner = s * 0.26f
        val toothOuter = s * 0.38f
        val w = s * 0.16f
        drawCircle(color, ringRadius, center, style = Stroke(w))
        repeat(8) { i ->
            val angle = i * PI.toFloat() / 4f
            val dir = Offset(cos(angle), sin(angle))
            drawLine(
                color,
                center + dir * toothInner,
                center + dir * toothOuter,
                w,
                StrokeCap.Round
            )
        }
    }
}

/** Rounded left chevron for back navigation. */
@Composable
fun ChevronLeftIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.64f, s * 0.17f)
            lineTo(s * 0.34f, s * 0.50f)
            lineTo(s * 0.64f, s * 0.83f)
        }
        drawPath(
            path, color,
            style = Stroke(s * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Small diagonal arrow shown in the "arrows left" pill. */
@Composable
fun CountArrowIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.14f
        drawLine(color, Offset(s * 0.24f, s * 0.76f), Offset(s * 0.74f, s * 0.26f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.46f, s * 0.26f), Offset(s * 0.74f, s * 0.26f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.74f, s * 0.26f), Offset(s * 0.74f, s * 0.54f), w, StrokeCap.Round)
    }
}

/** Five-point star for level ratings; filled when earned. */
@Composable
fun StarIcon(
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(s / 2f, s * 0.53f)
        val outer = s * 0.47f
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = (-PI / 2 + i * PI / 5).toFloat()
            val x = center.x + cos(angle) * radius
            val y = center.y + sin(angle) * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) drawPath(path, color)
        else drawPath(path, color, style = Stroke(s * 0.1f, join = StrokeJoin.Round))
    }
}

/** Solid play triangle for the home bottom bar. */
@Composable
fun PlayIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.35f, s * 0.20f)
            lineTo(s * 0.80f, s * 0.50f)
            lineTo(s * 0.35f, s * 0.80f)
            close()
        }
        drawPath(path, color)
    }
}

/** 2x2 rounded tiles standing in for the level map. */
@Composable
fun TilesIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val gap = s * 0.12f
        val cell = (s - gap) / 2f
        val corner = CornerRadius(cell * 0.30f, cell * 0.30f)
        for (row in 0..1) {
            for (col in 0..1) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = corner
                )
            }
        }
    }
}

/** Padlock for locked level tiles. */
@Composable
fun LockIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.14f
        val bodyTop = s * 0.46f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.18f, bodyTop),
            size = Size(s * 0.64f, s * 0.40f),
            cornerRadius = CornerRadius(s * 0.10f)
        )
        val shackle = Path().apply {
            moveTo(s * 0.32f, bodyTop)
            lineTo(s * 0.32f, s * 0.34f)
            cubicTo(s * 0.32f, s * 0.12f, s * 0.68f, s * 0.12f, s * 0.68f, s * 0.34f)
            lineTo(s * 0.68f, bodyTop)
        }
        drawPath(shackle, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Rounded check mark for completed daily puzzles. */
@Composable
fun CheckIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.24f, s * 0.53f)
            lineTo(s * 0.43f, s * 0.72f)
            lineTo(s * 0.77f, s * 0.30f)
        }
        drawPath(
            path, color,
            style = Stroke(s * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Monthly trophy cup; filled when the tier is earned. */
@Composable
fun TrophyIcon(
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.10f

        val cup = Path().apply {
            moveTo(s * 0.30f, s * 0.18f)
            lineTo(s * 0.70f, s * 0.18f)
            cubicTo(s * 0.70f, s * 0.44f, s * 0.62f, s * 0.57f, s * 0.50f, s * 0.57f)
            cubicTo(s * 0.38f, s * 0.57f, s * 0.30f, s * 0.44f, s * 0.30f, s * 0.18f)
            close()
        }
        val stem = Path().apply {
            moveTo(s * 0.50f, s * 0.57f)
            lineTo(s * 0.50f, s * 0.69f)
            moveTo(s * 0.42f, s * 0.70f)
            lineTo(s * 0.58f, s * 0.70f)
            moveTo(s * 0.35f, s * 0.81f)
            lineTo(s * 0.65f, s * 0.81f)
        }
        val handles = Path().apply {
            moveTo(s * 0.30f, s * 0.25f)
            cubicTo(s * 0.12f, s * 0.25f, s * 0.14f, s * 0.47f, s * 0.34f, s * 0.47f)
            moveTo(s * 0.70f, s * 0.25f)
            cubicTo(s * 0.88f, s * 0.25f, s * 0.86f, s * 0.47f, s * 0.66f, s * 0.47f)
        }

        if (filled) drawPath(cup, color) else drawPath(cup, color, style = Stroke(w, join = StrokeJoin.Round))
        drawPath(stem, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(handles, color, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** Small flame used for the daily streak. */
@Composable
fun FlameIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val flame = Path().apply {
            moveTo(s * 0.50f, s * 0.10f)
            cubicTo(s * 0.62f, s * 0.28f, s * 0.79f, s * 0.37f, s * 0.79f, s * 0.58f)
            cubicTo(s * 0.79f, s * 0.79f, s * 0.66f, s * 0.91f, s * 0.50f, s * 0.91f)
            cubicTo(s * 0.34f, s * 0.91f, s * 0.21f, s * 0.79f, s * 0.21f, s * 0.58f)
            cubicTo(s * 0.21f, s * 0.44f, s * 0.31f, s * 0.35f, s * 0.39f, s * 0.25f)
            cubicTo(s * 0.40f, s * 0.40f, s * 0.45f, s * 0.47f, s * 0.52f, s * 0.52f)
            cubicTo(s * 0.47f, s * 0.36f, s * 0.49f, s * 0.20f, s * 0.50f, s * 0.10f)
            close()
        }
        drawPath(flame, color)
    }
}

/** Counter-clockwise arrow for undo. */
@Composable
fun UndoIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.12f
        val arc = Path().apply {
            moveTo(s * 0.27f, s * 0.55f)
            cubicTo(s * 0.27f, s * 0.30f, s * 0.50f, s * 0.20f, s * 0.67f, s * 0.30f)
            cubicTo(s * 0.84f, s * 0.40f, s * 0.84f, s * 0.68f, s * 0.63f, s * 0.78f)
        }
        drawPath(arc, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val head = Path().apply {
            moveTo(s * 0.41f, s * 0.40f)
            lineTo(s * 0.25f, s * 0.55f)
            lineTo(s * 0.44f, s * 0.66f)
        }
        drawPath(head, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Two rounded bars for pause. */
@Composable
fun PauseIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.17f
        val h = s * 0.58f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.30f, s * 0.21f),
            size = Size(w, h),
            cornerRadius = CornerRadius(w / 2f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.53f, s * 0.21f),
            size = Size(w, h),
            cornerRadius = CornerRadius(w / 2f)
        )
    }
}

/** Waste bin for the destructive reset row. */
@Composable
fun TrashIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.10f
        val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(s * 0.18f, s * 0.26f), Offset(s * 0.82f, s * 0.26f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.40f, s * 0.16f), Offset(s * 0.60f, s * 0.16f), w, StrokeCap.Round)
        val body = Path().apply {
            moveTo(s * 0.26f, s * 0.30f)
            lineTo(s * 0.31f, s * 0.84f)
            cubicTo(s * 0.32f, s * 0.88f, s * 0.35f, s * 0.90f, s * 0.39f, s * 0.90f)
            lineTo(s * 0.61f, s * 0.90f)
            cubicTo(s * 0.65f, s * 0.90f, s * 0.68f, s * 0.88f, s * 0.69f, s * 0.84f)
            lineTo(s * 0.74f, s * 0.30f)
        }
        drawPath(body, color, style = stroke)
        drawLine(color, Offset(s * 0.43f, s * 0.42f), Offset(s * 0.44f, s * 0.74f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.57f, s * 0.42f), Offset(s * 0.56f, s * 0.74f), w, StrokeCap.Round)
    }
}
