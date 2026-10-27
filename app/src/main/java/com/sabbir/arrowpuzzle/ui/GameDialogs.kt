package com.sabbir.arrowpuzzle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sabbir.arrowpuzzle.GameConfig
import kotlinx.coroutines.delay

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

/** Level puzzle finished. */
@Composable
fun VictoryDialog(
    levelId: Int,
    moves: Int,
    elapsedSeconds: Int,
    errors: Int,
    hintsUsed: Int,
    onNext: () -> Unit,
    onReplay: () -> Unit
) {
    val stars = GameConfig.starsFor(errors, hintsUsed)

    DialogShell(onDismissRequest = {}) {
        DialogBadge(GameConfig.Accent) {
            StarIcon(filled = true, color = GameConfig.Accent, size = 34.dp)
        }

        Spacer(Modifier.height(4.dp))

        Eyebrow(
            text = "LEVEL COMPLETE",
            color = MaterialTheme.colorScheme.primary
        )
        DialogTitle("Level $levelId")

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
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val earned = index < stars
                StarIcon(
                    filled = earned,
                    color = if (earned) Color(0xFFFBBF24)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                    size = 32.dp,
                    modifier = Modifier.scale(starScales[index].value)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (stars) {
                3 -> "Flawless — no mistakes, no hints"
                2 -> "Solved with a little help"
                else -> "Solved"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(6.dp))

        val statAlphas = remember { List(3) { Animatable(0f) } }
        LaunchedEffect(Unit) {
            statAlphas.forEachIndexed { i, anim ->
                delay(i * 100L)
                anim.animateTo(1f, tween(200))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile("Moves", "$moves", Modifier.alpha(statAlphas[0].value))
            StatTile("Time", "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60), Modifier.alpha(statAlphas[1].value))
            StatTile("Mistakes", "$errors", Modifier.alpha(statAlphas[2].value))
        }

        Spacer(Modifier.height(6.dp))

        PrimaryPill(onClick = onNext) {
            Text(
                text = if (levelId == GameConfig.MAX_LEVELS) "SEE ALL LEVELS" else "NEXT LEVEL",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        TertiaryAction(text = "REPLAY LEVEL", onClick = onReplay)
    }
}

/** Pause menu; the board underneath keeps its state. */
@Composable
fun PauseDialog(
    levelLabel: String,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    DialogShell(onDismissRequest = onResume) {
        DialogBadge(GameConfig.Accent) {
            PauseGlyph(color = GameConfig.Accent, size = 28.dp)
        }

        Spacer(Modifier.height(4.dp))

        Eyebrow("PAUSED", GameConfig.Accent)
        DialogTitle("Take a break")
        DialogSubtitle("$levelLabel is safe — the board stays exactly where you left it.")

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
