package com.sabbir.arrowpuzzle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.data.PlayerPrefs
import com.sabbir.arrowpuzzle.ui.theme.ArrowPuzzleTheme
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember

/** Levels are listed in blocks so long runs feel like chapters, not one wall. */
private const val CHAPTER_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsScreen(
    prefs: () -> PlayerPrefs,
    onBack: () -> Unit,
    onLevelSelect: (Int) -> Unit,
    showBack: Boolean = true
) {
    val p = prefs()
    val totalLevels = GameConfig.MAX_LEVELS
    val completedCount = p.completed.count { it in 1..totalLevels }
    val totalStars = p.bestStars.filterKeys { it in 1..totalLevels }.values.sum()
    val percent = (completedCount * 100f / totalLevels).roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (showBack) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Levels",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$completedCount of $totalLevels solved",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        CircleIconButton(onClick = onBack) {
                            ChevronLeftIcon(
                                color = MaterialTheme.colorScheme.onSurface,
                                size = 16.dp
                            )
                        }
                    }
                },
                windowInsets = if (showBack) TopAppBarDefaults.windowInsets else WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "progress") {
                ProgressOverview(percent, completedCount, totalStars, totalLevels)
            }

            var start = 1
            while (start <= totalLevels) {
                val end = minOf(start + CHAPTER_SIZE - 1, totalLevels)
                val chapterStart = start

                item(span = { GridItemSpan(maxLineSpan) }, key = "chapter-$chapterStart") {
                    val solved = (chapterStart..end).count { it in p.completed }
                    ChapterHeader(chapterStart, end, solved)
                }

                items((chapterStart..end).toList(), key = { it }) { levelNum ->
                    LevelTile(
                        levelNum = levelNum,
                        isLocked = levelNum > p.unlocked,
                        isCompleted = levelNum in p.completed,
                        isCurrent = levelNum == p.unlocked,
                        stars = p.bestStars[levelNum] ?: 0,
                        onClick = { if (levelNum <= p.unlocked) onLevelSelect(levelNum) }
                    )
                }

                start = end + 1
            }
        }
    }
}

@Composable
private fun ProgressOverview(percent: Int, solved: Int, stars: Int, total: Int) {
    FlatCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your progress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$percent%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GameConfig.Accent
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = GameConfig.Accent,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Spacer(Modifier.height(14.dp))

            Row {
                MiniStat("Solved", "$solved", GameConfig.Accent)
                MiniStat("Stars", "$stars", Color(0xFFF59E0B))
                MiniStat("To go", "${total - solved}", MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ChapterHeader(start: Int, end: Int, solved: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Levels $start–$end",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$solved/${end - start + 1} solved",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun LevelTile(
    levelNum: Int,
    isLocked: Boolean,
    isCompleted: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.onSurface
    val container = when {
        isCurrent -> GameConfig.Accent
        isCompleted -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val contentColor = when {
        isCurrent -> Color.White
        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        else -> surface
    }
    val border = when {
        isCurrent -> null
        isCompleted -> BorderStroke(1.dp, GameConfig.Success.copy(alpha = 0.30f))
        else -> BorderStroke(1.dp, surface.copy(alpha = 0.07f))
    }

    // Task 6: Press scale feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed && !isLocked) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tile_press_scale"
    )

    Surface(
        onClick = onClick,
        enabled = !isLocked,
        interactionSource = interactionSource,
        modifier = Modifier
            .aspectRatio(1f)
            .scale(tileScale),
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = contentColor,
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = levelNum.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                when {
                    isLocked -> LockIcon(color = contentColor, size = 11.dp)
                    isCurrent -> Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "PLAY",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                    isCompleted && stars <= 0 -> CheckIcon(color = GameConfig.Success, size = 12.dp)
                    isCompleted -> Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val earned = index < stars
                            StarIcon(
                                filled = earned,
                                color = if (earned) Color(0xFFF59E0B) else surface.copy(alpha = 0.15f),
                                size = 9.dp
                            )
                        }
                    }
                    else -> Text(
                        text = "OPEN",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelsScreenPreview() {
    ArrowPuzzleTheme {
        LevelsScreen(
            prefs = { PlayerPrefs(unlocked = 5, completed = setOf(1, 2, 3, 4), bestStars = mapOf(1 to 3, 2 to 2, 3 to 1, 4 to 3)) },
            onBack = {},
            onLevelSelect = {}
        )
    }
}
