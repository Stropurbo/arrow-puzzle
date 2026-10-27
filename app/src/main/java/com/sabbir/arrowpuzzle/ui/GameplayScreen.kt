package com.sabbir.arrowpuzzle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.model.ArrowPiece
import com.sabbir.arrowpuzzle.model.Difficulty
import com.sabbir.arrowpuzzle.model.Direction
import com.sabbir.arrowpuzzle.model.GameState
import com.sabbir.arrowpuzzle.model.GridCell
import com.sabbir.arrowpuzzle.model.LevelDefinition
import com.sabbir.arrowpuzzle.ui.theme.ArrowPuzzleTheme
import com.sabbir.arrowpuzzle.vm.GameViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class Ripple(
    val key: Long,
    val pos: Offset,
    val anim: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>
)

private data class Confetto(
    val x0: Float,
    val y0: Float,
    val speed: Float,
    val drift: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameplayScreen(
    vm: GameViewModel,
    onBack: () -> Unit,
    onLevels: () -> Unit,
    onNext: () -> Unit
) {
    val state by vm.game.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val s = state

    DisposableEffect(vm) {
        vm.setGameVisible(true)
        onDispose { vm.setGameVisible(false) }
    }

    // A process death can restore Screen.GAME while the ViewModel is empty.
    // Restore the saved board instead of showing a spinner forever.
    LaunchedEffect(s) {
        if (s == null) vm.continueGame()
    }

    if (s == null) {
        BackHandler { onBack() }
        LoadingBoard(preparingDaily = false)
        return
    }

    val reduced = prefs.reducedMotion
    val haptics = LocalHapticFeedback.current
    var lastHaptic by remember { mutableLongStateOf(0L) }

    fun throttledHaptic(type: HapticFeedbackType) {
        if (!prefs.haptics) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastHaptic < GameConfig.HAPTIC_THROTTLE_MS) return
        lastHaptic = now
        haptics.performHapticFeedback(type)
    }

    LaunchedEffect(s.movingPieceId) {
        if (s.movingPieceId != null && !reduced) throttledHaptic(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(s.invalidPieceId) {
        if (s.invalidPieceId != null) throttledHaptic(HapticFeedbackType.Reject)
    }
    LaunchedEffect(s.completed) {
        if (s.completed) throttledHaptic(HapticFeedbackType.LongPress)
    }

    // Exit slide animation.
    val movingPiece = s.level.pieces.find { it.id == s.movingPieceId }
    val exitDur = if (movingPiece != null && !reduced)
        GameConfig.exitDurationMs(movingPiece.cells.size, s.level.rows, s.level.cols) else 1
    val exitT = remember { Animatable(0f) }
    LaunchedEffect(s.movingPieceId) {
        if (s.movingPieceId != null && !reduced) {
            exitT.snapTo(0f)
            exitT.animateTo(1f, tween(exitDur, easing = FastOutLinearInEasing))
        } else exitT.snapTo(0f)
    }

    // Undo reverse slide-in animation.
    val undoingPiece = s.level.pieces.find { it.id == s.undoingPieceId }
    val undoDur = if (undoingPiece != null && !reduced)
        GameConfig.exitDurationMs(undoingPiece.cells.size, s.level.rows, s.level.cols) else 1
    val undoT = remember { Animatable(0f) }
    LaunchedEffect(s.undoingPieceId) {
        if (s.undoingPieceId != null && !reduced) {
            undoT.snapTo(1f)
            undoT.animateTo(0f, tween(undoDur, easing = FastOutSlowInEasing))
        } else undoT.snapTo(0f)
    }

    // Invalid shake animation.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(s.invalidPieceId, s.errors) {
        if (s.invalidPieceId != null && !reduced) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(GameConfig.INVALID_ANIMATION_MS))
        } else shake.snapTo(0f)
    }

    // Hint pulse.
    val hintedId = s.hintedPieceId
    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val pulse: Float = if (hintedId != null && !reduced) {
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse"
        ).value
    } else 1f

    // Per-heart scale animatables for individual heart pop on error
    val heartScales = remember(s.level.allowedMistakes) {
        List(s.level.allowedMistakes) { Animatable(1f) }
    }
    LaunchedEffect(s.errors) {
        if (s.errors > 0 && !reduced) {
            // The heart that just became empty is at index s.hearts (0-based empty heart)
            val idx = s.hearts.coerceIn(0, heartScales.size - 1)
            heartScales[idx].snapTo(1.4f)
            heartScales[idx].animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Grid Overlay state (default from GameConfig)
    var showGridOverlay by remember { mutableStateOf(GameConfig.GRID_OVERLAY_DEFAULT) }

    // Tap ripples.
    var ripples by remember(s.level.id) { mutableStateOf(listOf<Ripple>()) }
    var rippleSeq by remember(s.level.id) { mutableLongStateOf(0L) }

    ripples.forEach { r ->
        LaunchedEffect(r.key) {
            try {
                r.anim.animateTo(1f, tween(380))
            } finally {
                ripples = ripples.filter { it.key != r.key }
            }
        }
    }

    val hintsLeft = (GameConfig.HINTS_PER_LEVEL - s.hintsUsed).coerceAtLeast(0)
    val canUndo = s.history.isNotEmpty() && s.movingPieceId == null && s.undoingPieceId == null &&
        !s.paused && !s.completed && !s.outOfHearts

    // Back during play pauses the board; dialogs and the victory card already
    // consume back themselves.
    BackHandler(enabled = !s.paused && !s.completed && !s.outOfHearts) { vm.togglePause() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        onClick = {
                            vm.audio.play("click")
                            vm.leaveGame()
                            onBack()
                        },
                        contentDescription = "Back to menu"
                    ) {
                        ChevronLeftIcon(color = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Level ${s.level.id}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${s.level.rows}×${s.level.cols} • ${s.level.pieces.size} arrows",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }

                    CircleIconButton(
                        onClick = vm::togglePause,
                        contentDescription = "Pause"
                    ) {
                        PauseIcon(color = MaterialTheme.colorScheme.onSurface, size = 17.dp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatPill {
                        CountArrowIcon(color = MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
                        Spacer(Modifier.width(6.dp))
                        AnimatedContent(
                            targetState = s.remainingIds.size,
                            transitionSpec = {
                                (slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it } + fadeIn(tween(160)))
                                    .togetherWith(slideOutVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it } + fadeOut(tween(160)))
                            },
                            label = "arrow_count"
                        ) { count ->
                            Text(
                                text = "$count",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(s.level.allowedMistakes) { index ->
                            val active = index < s.hearts
                            HeartIcon(
                                filled = active,
                                color = if (active) GameConfig.Danger
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                size = 20.dp,
                                modifier = Modifier.scale(heartScales.getOrNull(index)?.value ?: 1f)
                            )
                        }
                    }

                    StatPill {
                        Text(
                            text = difficultyLabel(s.level.difficulty),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PuzzleCanvas(
                        state = s,
                        largeHit = prefs.largeHitArea,
                        highContrast = prefs.highContrast,
                        reduced = reduced,
                        showGrid = showGridOverlay,
                        exitId = s.movingPieceId,
                        exitT = exitT.value,
                        undoId = s.undoingPieceId,
                        undoT = undoT.value,
                        shakeV = shake.value,
                        pulse = pulse,
                        ripples = ripples
                    ) { pos, hitId ->
                        if (!reduced) {
                            rippleSeq++
                            val r = Ripple(rippleSeq, pos, Animatable(0f))
                            ripples = (ripples + r).takeLast(5)
                        }
                        if (hitId != null) vm.tap(hitId)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Hint Button + Label
                    val showHintCta = hintsLeft > 0 && s.movingPieceId == null && s.moves == 0
                    val hintCtaTransition = rememberInfiniteTransition(label = "hint_cta")
                    val hintCtaScale by if (showHintCta && !reduced) {
                        hintCtaTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.07f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                            label = "hint_cta_scale"
                        )
                    } else {
                        remember { mutableStateOf(1f) }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgedBox(
                            modifier = Modifier.scale(hintCtaScale),
                            badge = {
                                if (hintsLeft > 0) {
                                    Badge(
                                        containerColor = GameConfig.HintBlue,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            hintsLeft.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            DockButton(
                                onClick = vm::hint,
                                enabled = hintsLeft > 0 && s.movingPieceId == null,
                                contentDescription = "Show a hint"
                            ) {
                                HintIcon(
                                    color = if (hintsLeft > 0) GameConfig.HintBlue
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                    size = 24.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Hint",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hintsLeft > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    // 2. Undo / Reverse Button + Label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DockButton(
                            onClick = vm::undo,
                            enabled = canUndo,
                            contentDescription = "Undo last move"
                        ) {
                            UndoIcon(
                                color = if (canUndo) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                size = 22.dp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Undo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    // 3. Dot Grid Toggle Button + Label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DockButton(
                            onClick = {
                                showGridOverlay = !showGridOverlay
                                vm.audio.play("click")
                            },
                            active = showGridOverlay,
                            contentDescription = "Toggle dot grid"
                        ) {
                            GridToggleIcon(
                                color = if (showGridOverlay) GameConfig.Accent
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                size = 22.dp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Grid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showGridOverlay) GameConfig.Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (s.completed && !reduced) ConfettiOverlay()
        }
    }

    // Overlays & Dialogs
    when {
        s.paused -> PauseDialog(
            levelLabel = "Level ${s.level.id}",
            onResume = vm::togglePause,
            onRestart = vm::restart,
            onHome = {
                vm.leaveGame()
                onBack()
            }
        )

        s.outOfHearts -> GameOverDialog(
            onContinue = vm::continueHearts,
            onRestart = vm::restart
        )

        s.completed -> VictoryDialog(
            levelId = s.level.id,
            moves = s.moves,
            elapsedSeconds = s.elapsedSeconds,
            errors = s.errors,
            hintsUsed = s.hintsUsed,
            onNext = if (s.level.id == GameConfig.MAX_LEVELS) onLevels else onNext,
            onReplay = vm::restart
        )
    }
}

/** Small rounded readout in the top HUD. */
@Composable
private fun StatPill(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

/** Circular action in the bottom dock. */
@Composable
private fun DockButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_press_scale"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(54.dp)
            .scale(pressScale)
            .shadow(
                elevation = if (enabled) 5.dp else 0.dp,
                shape = CircleShape,
                spotColor = GameConfig.Accent.copy(alpha = 0.35f)
            )
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = if (active) GameConfig.Accent.copy(alpha = 0.13f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (active) GameConfig.Accent.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun LoadingBoard(preparingDaily: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = GameConfig.Accent, strokeWidth = 3.dp)
            Text(
                text = if (preparingDaily) "Preparing today's puzzle…" else "Loading puzzle…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.TUTORIAL, Difficulty.EASY -> "Easy"
    Difficulty.MEDIUM -> "Medium"
    Difficulty.CHALLENGING, Difficulty.HARD -> "Hard"
    Difficulty.EXPERT -> "Expert"
}

/**
 * Smoothly slithers an arrow along its curved track using PathMeasure like a fluid progress bar.
 * Preserves exact 90-degree rounded corners without any geometrical distortion or popping.
 */
private fun buildSlitheringArrowPath(
    pts: List<Offset>,
    corner: Float,
    dir: Direction,
    cell: Float,
    progress: Float,
    canvasSize: Size
): Path? {
    if (pts.isEmpty()) return null
    if (progress <= 0f) return buildArrowPath(pts, corner, dir, cell)

    val lastPt = pts.last()
    // Distance to ensure arrow flies completely past the visible canvas
    val canvasExitDist = when (dir) {
        Direction.RIGHT -> (canvasSize.width - lastPt.x) + cell * 3f
        Direction.LEFT -> lastPt.x + cell * 3f
        Direction.DOWN -> (canvasSize.height - lastPt.y) + cell * 3f
        else -> lastPt.y + cell * 3f
    }

    // Extended points track
    val extendedPts = pts.toMutableList()
    val farExitPt = Offset(
        lastPt.x + dir.dc.toFloat() * (canvasExitDist + cell * 2f),
        lastPt.y + dir.dr.toFloat() * (canvasExitDist + cell * 2f)
    )
    extendedPts.add(farExitPt)

    // Build the continuous smooth track with rounded corners
    val fullTrack = roundedPolylinePath(extendedPts, corner)
    val pm = PathMeasure()
    pm.setPath(fullTrack, false)
    val trackLen = pm.length

    // Measure the original arrow length
    val origTrack = roundedPolylinePath(pts, corner)
    val origPm = PathMeasure()
    origPm.setPath(origTrack, false)
    val arrowLen = max(origPm.length, cell * 0.5f)

    val totalTravel = arrowLen + canvasExitDist
    val startD = totalTravel * progress
    val stopD = min(arrowLen + totalTravel * progress, trackLen)

    if (stopD <= startD || startD >= trackLen) return null

    val path = Path()
    pm.getSegment(startD, stopD, path, true)

    // Position & tangent for the arrowhead
    val headPos = pm.getPosition(stopD)
    val headTan = pm.getTangent(stopD)
    val tanLen = headTan.getDistance()
    val v = if (tanLen > 0.001f) {
        Offset(headTan.x / tanLen, headTan.y / tanLen)
    } else {
        Offset(dir.dc.toFloat(), dir.dr.toFloat())
    }
    val side = Offset(-v.y, v.x)
    val headLen = cell * GameConfig.ARROW_HEAD_LENGTH_FRACTION
    val headHalf = cell * GameConfig.ARROW_HEAD_HALF_WIDTH_FRACTION
    val base = Offset(headPos.x - v.x * headLen, headPos.y - v.y * headLen)

    path.moveTo(base.x + side.x * headHalf, base.y + side.y * headHalf)
    path.lineTo(headPos.x, headPos.y)
    path.lineTo(base.x - side.x * headHalf, base.y - side.y * headHalf)

    return path
}

@Composable
private fun PuzzleCanvas(
    state: GameState,
    largeHit: Boolean,
    highContrast: Boolean,
    reduced: Boolean,
    showGrid: Boolean,
    exitId: String?,
    exitT: Float,
    undoId: String?,
    undoT: Float,
    shakeV: Float,
    pulse: Float,
    ripples: List<Ripple>,
    onTap: (Offset, String?) -> Unit
) {
    val arrowColor = MaterialTheme.colorScheme.onBackground
    val gapColor = MaterialTheme.colorScheme.background
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (highContrast) 0.32f else 0.14f
    )
    val latestTap by rememberUpdatedState(onTap)

    // Heading lookup is derived data; compute once per level instead of per frame.
    val headings = remember(state.level.id) {
        state.level.pieces.associateBy({ it.id }, { runCatching { it.heading }.getOrNull() })
    }

    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(state.level.id) {
        if (reduced) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(600))  // 3c: extended to 600ms for stagger effect
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clipToBounds()
            .semantics { contentDescription = "Puzzle board level ${state.level.id}" }
            .pointerInput(state.level.id, state.remainingIds, largeHit) {
                detectTapGestures { tap ->
                    val cell = min(
                        size.width.toFloat() / state.level.cols,
                        size.height.toFloat() / state.level.rows
                    ) * 0.88f
                    if (cell <= 0f) {
                        latestTap(tap, null); return@detectTapGestures
                    }
                    val ox = (size.width - cell * state.level.cols) / 2f
                    val oy = (size.height - cell * state.level.rows) / 2f
                    val radius = if (largeHit) cell * 0.58f else cell * 0.46f
                    val candidates = state.level.pieces.filter { it.id in state.remainingIds }
                    val hit = candidates.minByOrNull { piece ->
                        distanceToPiece(tap, piece, ox, oy, cell)
                    }
                    latestTap(
                        tap,
                        if (hit != null && distanceToPiece(tap, hit, ox, oy, cell) <= radius) hit.id else null
                    )
                }
            }
    ) {
        val cell = min(size.width / state.level.cols, size.height / state.level.rows) * 0.88f
        if (cell <= 0f) return@Canvas
        val ox = (size.width - cell * state.level.cols) / 2f
        val oy = (size.height - cell * state.level.rows) / 2f
        val boardRight = ox + cell * state.level.cols
        val boardBottom = oy + cell * state.level.rows

        if (showGrid) {
            val dotRadius = (cell * GameConfig.DOT_RADIUS_FRACTION).coerceIn(2.5f, 4.5f)
            for (r in 0 until state.level.rows) {
                for (c in 0 until state.level.cols) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(ox + (c + .5f) * cell, oy + (r + .5f) * cell)
                    )
                }
            }
        }

        val stroke = cell * if (highContrast) GameConfig.ARROW_STROKE_FRACTION_HC
        else GameConfig.ARROW_STROKE_FRACTION
        val corner = min(cell * GameConfig.ARROW_CORNER_FRACTION, stroke * 1.6f)
        val visible = state.level.pieces.filter { it.id in state.remainingIds }
        val ordered = visible.sortedBy { if (it.id == exitId || it.id == undoId) 1 else 0 }

        ordered.forEachIndexed { index, p ->
            val isExit = p.id == exitId
            val isUndo = p.id == undoId
            val isHinted = p.id == state.hintedPieceId
            val isInvalid = p.id == state.invalidPieceId
            val baseColor = when {
                isExit -> GameConfig.Accent
                isUndo -> GameConfig.Accent
                isHinted -> GameConfig.HintBlue
                isInvalid -> GameConfig.Danger
                else -> arrowColor
            }

            // 3c: Per-arrow staggered reveal alpha
            val revealAlpha = (reveal.value - index * 0.04f).coerceIn(0f, 1f)
            val exitAlpha = if (isExit) (1f - (exitT - 0.82f) / 0.18f).coerceIn(0f, 1f) else 1f
            val finalAlpha = revealAlpha * exitAlpha

            var pts = p.cells.map {
                Offset(ox + (it.col + .5f) * cell, oy + (it.row + .5f) * cell)
            }
            val dir = headings[p.id] ?: return@forEachIndexed

            if (isInvalid && shakeV > 0f && shakeV < 1f) {
                val nudgeDist = sin(shakeV * PI.toFloat() * 4) * cell * 0.18f * (1f - shakeV)
                val dx = dir.dc * nudgeDist
                val dy = dir.dr * nudgeDist
                pts = pts.map { Offset(it.x + dx, it.y + dy) }
            }

            // Build smooth curved progress-bar path (slithers effortlessly around bends) or static path
            val arrowPath = when {
                isExit && exitT > 0f -> buildSlitheringArrowPath(pts, corner, dir, cell, exitT, size) ?: return@forEachIndexed
                isUndo && undoT > 0f -> buildSlitheringArrowPath(pts, corner, dir, cell, undoT, size) ?: return@forEachIndexed
                else -> buildArrowPath(pts, corner, dir, cell)
            }

            // 3a: Arrow selection highlight flash when just-started exit
            if (isExit && exitT < 0.08f) {
                val selAlpha = GameConfig.SELECTION_HIGHLIGHT_ALPHA * (1f - exitT / 0.08f)
                drawPath(
                    path = arrowPath,
                    color = GameConfig.Accent.copy(alpha = selAlpha),
                    style = Stroke(
                        width = stroke * 4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3b: Richer exit animation — 3D lift-off elevation + glowing energy trail
            if (isExit) {
                val lift = sin(exitT * PI.toFloat()).coerceAtLeast(0f)
                val trailFade = (1f - exitT).coerceIn(0f, 1f) * exitAlpha

                // Elevation drop shadow (makes the arrow look like it lifted off the board into the air)
                if (lift > 0.05f) {
                    withTransform({
                        translate(cell * 0.07f * lift, cell * 0.11f * lift)
                    }) {
                        drawPath(
                            path = arrowPath,
                            color = Color.Black.copy(alpha = 0.15f * lift * exitAlpha),
                            style = Stroke(width = stroke * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // Outermost glow
                drawPath(
                    path = arrowPath,
                    color = GameConfig.Accent.copy(alpha = 0.14f * trailFade),
                    style = Stroke(width = stroke * 5.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Middle halo
                drawPath(
                    path = arrowPath,
                    color = GameConfig.Accent.copy(alpha = 0.26f * trailFade),
                    style = Stroke(width = stroke * 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inner bright core
                drawPath(
                    path = arrowPath,
                    color = Color.White.copy(alpha = 0.50f * trailFade),
                    style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            if (isHinted) {
                drawPath(
                    path = arrowPath,
                    color = GameConfig.HintGlow.copy(alpha = 0.35f + 0.5f * pulse),
                    style = Stroke(
                        width = stroke * 3.3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawPath(
                    path = arrowPath,
                    color = gapColor,
                    style = Stroke(
                        width = stroke * 1.95f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            val currentStroke = if (isExit) stroke * (1f + 0.16f * sin(exitT * PI.toFloat()).coerceAtLeast(0f)) else stroke
            drawPath(
                path = arrowPath,
                color = baseColor.copy(alpha = finalAlpha),
                style = Stroke(width = currentStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 3i: Full-board invalid flash overlay
        if (state.invalidPieceId != null && shakeV > 0f) {
            val flashAlpha = (0.06f * sin(shakeV * PI.toFloat() * 2f)).toFloat().coerceAtLeast(0f)
            drawRect(
                color = GameConfig.Danger.copy(alpha = flashAlpha),
                topLeft = Offset(ox, oy),
                size = Size(cell * state.level.cols, cell * state.level.rows)
            )
        }

        ripples.forEach { r ->
            val a = 1f - r.anim.value
            if (a > 0f) drawCircle(
                GameConfig.Accent.copy(alpha = 0.5f * a),
                cell * (0.4f + 1.8f * r.anim.value),
                r.pos,
                style = Stroke(width = 5f)
            )
        }
    }
}

/**
 * One stroked path for a piece: rounded body plus an open V-shaped chevron head.
 */
private fun buildArrowPath(pts: List<Offset>, corner: Float, dir: Direction, cell: Float): Path {
    val path = roundedPolylinePath(pts, corner)
    if (pts.isEmpty()) return path
    val h = pts.last()
    val v = Offset(dir.dc.toFloat(), dir.dr.toFloat())
    val side = Offset(-v.y, v.x)
    val headLen = cell * GameConfig.ARROW_HEAD_LENGTH_FRACTION
    val headHalf = cell * GameConfig.ARROW_HEAD_HALF_WIDTH_FRACTION
    val base = Offset(h.x - v.x * headLen, h.y - v.y * headLen)
    path.moveTo(base.x + side.x * headHalf, base.y + side.y * headHalf)
    path.lineTo(h.x, h.y)
    path.lineTo(base.x - side.x * headHalf, base.y - side.y * headHalf)
    return path
}

/** Polyline with rounded 90-degree bends (quadratic corner cuts). */
private fun roundedPolylinePath(pts: List<Offset>, radius: Float): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size == 1) return path
    if (pts.size == 2) {
        path.lineTo(pts[1].x, pts[1].y); return path
    }
    for (i in 1 until pts.lastIndex) {
        val prev = pts[i - 1]
        val cur = pts[i]
        val next = pts[i + 1]
        val inVec = prev - cur
        val outVec = next - cur
        val inLen = inVec.getDistance()
        val outLen = outVec.getDistance()
        if (inLen < 0.001f || outLen < 0.001f) {
            path.lineTo(cur.x, cur.y)
            continue
        }
        val r = min(radius, min(inLen, outLen) * 0.5f)
        val entryX = cur.x + inVec.x / inLen * r
        val entryY = cur.y + inVec.y / inLen * r
        val exitX = cur.x + outVec.x / outLen * r
        val exitY = cur.y + outVec.y / outLen * r
        path.lineTo(entryX, entryY)
        path.quadraticTo(cur.x, cur.y, exitX, exitY)
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}

@Composable
private fun ConfettiOverlay() {
    val colors = listOf(
        GameConfig.Accent,
        GameConfig.Success,
        GameConfig.Danger,
        Color(0xFFFFC93C),  // gold
        Color(0xFF9B5DE5),  // purple
        Color(0xFF00B4D8)   // cyan
    )
    val parts = remember {
        val rng = kotlin.random.Random(7)
        List(GameConfig.CONFETTI_COUNT) { i ->
            Confetto(
                x0 = rng.nextFloat(),
                y0 = rng.nextFloat() * -0.4f,
                speed = rng.nextFloat() * 0.5f + 0.25f,
                drift = (rng.nextFloat() - 0.5f) * 0.3f,
                color = colors[i % colors.size],
                size = rng.nextFloat() * 12f + 6f,
                rotation = rng.nextFloat() * 360f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 360f
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3200, easing = LinearEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val p = t.value
        parts.forEachIndexed { index, c ->
            val y = (c.y0 + p * c.speed) % 1.25f
            val x = (c.x0 + p * c.drift + 1f) % 1f
            val cx = x * size.width
            val cy = y * size.height - 24f
            val angle = c.rotation + c.rotationSpeed * p
            // Alternate rectangle and triangle shapes by index
            val angleRad = angle * PI.toFloat() / 180f
            val cosA = cos(angleRad)
            val sinA = sin(angleRad)
            if (index % 2 == 0) {
                // Rectangle (parallelogram with rotation)
                val hw = c.size / 2f
                val hh = c.size * 0.35f
                val corners = listOf(
                    Offset(-hw, -hh), Offset(hw, -hh), Offset(hw, hh), Offset(-hw, hh)
                ).map { (ox, oy) ->
                    Offset(cx + ox * cosA - oy * sinA, cy + ox * sinA + oy * cosA)
                }
                val path = Path().apply {
                    moveTo(corners[0].x, corners[0].y)
                    lineTo(corners[1].x, corners[1].y)
                    lineTo(corners[2].x, corners[2].y)
                    lineTo(corners[3].x, corners[3].y)
                    close()
                }
                drawPath(path, c.color)
            } else {
                // Triangle
                val r = c.size * 0.6f
                val tri = listOf(0f, 120f, 240f).map { deg ->
                    val rad = (deg + angle) * PI.toFloat() / 180f
                    Offset(cx + r * cos(rad), cy + r * sin(rad))
                }
                val path = Path().apply {
                    moveTo(tri[0].x, tri[0].y)
                    lineTo(tri[1].x, tri[1].y)
                    lineTo(tri[2].x, tri[2].y)
                    close()
                }
                drawPath(path, c.color)
            }
        }
    }
}

private fun distanceToPiece(
    tap: Offset,
    piece: ArrowPiece,
    ox: Float,
    oy: Float,
    cell: Float
): Float {
    val pts = piece.cells.map { Offset(ox + (it.col + .5f) * cell, oy + (it.row + .5f) * cell) }
    if (pts.isEmpty()) return Float.MAX_VALUE
    if (pts.size == 1) return (tap - pts[0]).getDistance()
    var best = Float.MAX_VALUE
    for (i in 0 until pts.lastIndex) best = min(best, segmentDistance(tap, pts[i], pts[i + 1]))
    best = min(best, (tap - pts.last()).getDistance())
    return best
}

private fun segmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l = dx * dx + dy * dy
    if (l == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

@Preview(showBackground = true)
@Composable
private fun GameplayScreenPreview() {
    ArrowPuzzleTheme {
        val sampleLevel = LevelDefinition(
            id = 5,
            rows = 8,
            cols = 8,
            pieces = listOf(
                ArrowPiece("p1", listOf(GridCell(0, 1), GridCell(0, 5)), Direction.RIGHT),
                ArrowPiece("p2", listOf(GridCell(2, 6), GridCell(2, 2)), Direction.LEFT),
                ArrowPiece("p3", listOf(GridCell(1, 0), GridCell(5, 0)), Direction.DOWN)
            ),
            difficulty = Difficulty.EASY,
            allowedMistakes = 3,
            solution = listOf("p1", "p2", "p3")
        )
        val sampleState = GameState(level = sampleLevel)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChevronLeftIcon(color = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                Text(
                    "Level 5",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                PauseIcon(color = MaterialTheme.colorScheme.onSurface, size = 17.dp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill {
                    CountArrowIcon(color = MaterialTheme.colorScheme.onSurfaceVariant, size = 15.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("3", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { HeartIcon(filled = true, color = GameConfig.Danger, size = 20.dp) }
                }
                StatPill { Text("Easy", fontSize = 13.sp) }
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PuzzleCanvas(
                    state = sampleState,
                    largeHit = false,
                    highContrast = false,
                    reduced = false,
                    showGrid = true,
                    exitId = null,
                    exitT = 0f,
                    undoId = null,
                    undoT = 0f,
                    shakeV = 0f,
                    pulse = 1f,
                    ripples = emptyList(),
                    onTap = { _, _ -> }
                )
            }
        }
    }
}
