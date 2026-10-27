package com.sabbir.arrowpuzzle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.data.PlayerPrefs
import com.sabbir.arrowpuzzle.model.LevelDefinition
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.material3.Switch

private val Scrim = Color(0x8C0F172A)

/** Shared dim scrim + flat themed card used by every overlay. */
@Composable
private fun DialogShell(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content
                    )
                }
            }
        }
    }
}

/** Circular icon badge at the top of a dialog. 4b: spring bounce scale on enter. */
@Composable
private fun DialogBadge(tint: Color, content: @Composable () -> Unit) {
    val badgeScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        badgeScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(badgeScale.value)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Eyebrow(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.4.sp,
        color = color
    )
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DialogSubtitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/** Flat stat tile used in the victory card. */
@Composable
private fun RowScope.StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PrimaryPill(
    onClick: () -> Unit,
    containerColor: Color = GameConfig.Accent,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        content = content
    )
}

@Composable
private fun SecondaryPill(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun TertiaryAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

/** Hand-drawn pause mark so the pause card needs no extra icon dependency. */
@Composable
private fun PauseGlyph(color: Color, size: Dp = 30.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val barWidth = s * 0.17f
        val barHeight = s * 0.62f
        val radius = CornerRadius(barWidth / 2f)
        drawRoundRect(color, Offset(s * 0.26f, s * 0.19f), Size(barWidth, barHeight), radius)
        drawRoundRect(color, Offset(s * 0.57f, s * 0.19f), Size(barWidth, barHeight), radius)
    }
}

/**
 * Level puzzle finished — full-screen celebration in the reference video's
 * style: blue background, falling confetti, the solved board as a thumbnail
 * in a white card, stars and stats, then NEXT GAME / MAIN / REPLAY.
 * This is a full-screen overlay (not a centred dialog), so it is drawn on
 * top of the board in GameplayScreen's overlay slot.
 */
@Composable
fun VictoryDialog(
    level: LevelDefinition,
    moves: Int,
    elapsedSeconds: Int,
    errors: Int,
    hintsUsed: Int,
    onNext: () -> Unit,
    onHome: () -> Unit,
    onReplay: () -> Unit
) {
    val levelId = level.id
    val stars = GameConfig.starsFor(errors, hintsUsed)
    val blue = Color(0xFF2F6BFF)

    // Falling confetti, deterministic so previews and tests see the same layout.
    val confetti = remember {
        val rnd = Random(42)
        val colors = listOf(
            Color(0xFFFBBF24), Color(0xFFF472B6), Color.White,
            Color(0xFF34D399), Color(0xFFF97316), Color(0xFFA78BFA)
        )
        List(36) {
            ConfettiDot(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                size = 4f + rnd.nextFloat() * 6f,
                color = colors[rnd.nextInt(colors.size)]
            )
        }
    }
    val fall by rememberInfiniteTransition(label = "victoryConfetti").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "victoryFall"
    )

    val starScales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        starScales.forEachIndexed { i, anim ->
            delay(i * 150L)
            anim.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(blue)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Confetti layer behind the content.
        Canvas(modifier = Modifier.fillMaxSize()) {
            confetti.forEach { dot ->
                val y = ((dot.y + fall) % 1f) * size.height
                drawCircle(
                    color = dot.color.copy(alpha = 0.9f),
                    radius = dot.size,
                    center = Offset(dot.x * size.width, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Level Completed",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Level $levelId  •  " + when (stars) {
                    3 -> "Flawless — no mistakes, no hints"
                    2 -> "Solved with a little help"
                    else -> "Solved"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val earned = index < stars
                    StarIcon(
                        filled = earned,
                        color = if (earned) Color(0xFFFBBF24)
                        else Color.White.copy(alpha = 0.35f),
                        size = 34.dp,
                        modifier = Modifier.scale(starScales[index].value)
                    )
                }
            }

            // White thumbnail card with the solved board's arrows, like the video.
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniBoard(level = level, modifier = Modifier.fillMaxWidth().height(210.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat("Moves", "$moves")
                        MiniStat("Time", "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60))
                        MiniStat("Mistakes", "$errors")
                    }
                }
            }

            // Next Game: white pill, blue text — the video's primary button.
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = blue)
            ) {
                Text(
                    text = if (levelId == GameConfig.MAX_LEVELS) "SEE ALL LEVELS" else "NEXT GAME",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("MAIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
            TextButton(
                onClick = onReplay,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f))
            ) {
                Text("REPLAY LEVEL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            }
        }
    }
}

private data class ConfettiDot(val x: Float, val y: Float, val size: Float, val color: Color)

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color(0xFF64748B))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}

/** Small solved-board thumbnail: dot grid plus one dark polyline per arrow. */
@Composable
private fun MiniBoard(level: LevelDefinition, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cols = level.cols
        val rows = level.rows
        val cell = minOf(size.width / cols, size.height / rows)
        val ox = (size.width - cell * cols) / 2f
        val oy = (size.height - cell * rows) / 2f
        val playable = level.playableCells
        playable.forEach { cellPos ->
            drawCircle(
                color = Color(0xFFCBD5E1),
                radius = 2.5f,
                center = Offset(ox + (cellPos.col + .5f) * cell, oy + (cellPos.row + .5f) * cell)
            )
        }
        level.pieces.forEach { piece ->
            if (piece.cells.isEmpty()) return@forEach
            val path = Path()
            piece.cells.forEachIndexed { i, cellPos ->
                val x = ox + (cellPos.col + .5f) * cell
                val y = oy + (cellPos.row + .5f) * cell
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = Color(0xFF0F172A),
                style = Stroke(width = cell * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Pause menu; the board underneath keeps its state. With prefs supplied it
 * also carries the reference video's in-game quick settings (sound, music,
 * haptics) so the player never has to leave the game to change them.
 */
@Composable
fun PauseDialog(
    levelLabel: String,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    prefs: PlayerPrefs? = null,
    onPrefsChange: ((PlayerPrefs) -> PlayerPrefs) -> Unit = {}
) {
    DialogShell(onDismissRequest = onResume) {
        DialogBadge(GameConfig.Accent) {
            PauseGlyph(color = GameConfig.Accent, size = 28.dp)
        }

        Spacer(Modifier.height(4.dp))

        Eyebrow("PAUSED", GameConfig.Accent)
        DialogTitle("Take a break")
        DialogSubtitle("$levelLabel is safe — the board stays exactly where you left it.")

        if (prefs != null) {
            Spacer(Modifier.height(6.dp))
            QuickToggleRow("Sound", prefs.sound) { onPrefsChange { it.copy(sound = !it.sound) } }
            QuickToggleRow("Music", prefs.music) { onPrefsChange { it.copy(music = !it.music) } }
            QuickToggleRow("Haptics", prefs.haptics) { onPrefsChange { it.copy(haptics = !it.haptics) } }
        }

        Spacer(Modifier.height(10.dp))

        PrimaryPill(onClick = onResume) {
            Text("RESUME", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        SecondaryPill("RESTART PUZZLE", onRestart)
        TertiaryAction("MAIN MENU", onHome)
    }
}

/** All hearts spent. */
@Composable
fun GameOverDialog(onContinue: () -> Unit, onRestart: () -> Unit) {
    DialogShell(onDismissRequest = {}) {
        DialogBadge(GameConfig.Danger) {
            HeartIcon(filled = false, color = GameConfig.Danger, size = 32.dp)
        }

        Spacer(Modifier.height(4.dp))

        Eyebrow("OUT OF HEARTS", GameConfig.Danger)
        DialogTitle("So close!")
        DialogSubtitle(
            "That was one wrong tap too many. Top up with " +
                "${GameConfig.INITIAL_HEARTS} hearts, or start the board again."
        )

        Spacer(Modifier.height(10.dp))

        PrimaryPill(onClick = onContinue) {
            Text("CONTINUE", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            HeartIcon(filled = true, color = Color.White, size = 15.dp)
            Spacer(Modifier.width(4.dp))
            Text("×${GameConfig.INITIAL_HEARTS}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        SecondaryPill("RESTART LEVEL", onRestart)
    }
}

@Composable
private fun QuickToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

/** Flat yes/no confirmation, used for destructive actions like resetting progress. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    icon: @Composable () -> Unit
) {
    val tint = if (destructive) GameConfig.Danger else GameConfig.Accent
    DialogShell(onDismissRequest = onDismiss) {
        DialogBadge(tint) { icon() }

        Spacer(Modifier.height(4.dp))

        Eyebrow(if (destructive) "PLEASE CONFIRM" else "CONFIRM", tint)
        DialogTitle(title)
        DialogSubtitle(message)

        Spacer(Modifier.height(10.dp))

        PrimaryPill(onClick = onConfirm, containerColor = tint) {
            Text(confirmLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        SecondaryPill("CANCEL", onDismiss)
    }
}
