package com.sabbir.arrowpuzzle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.R
import com.sabbir.arrowpuzzle.data.PlayerPrefs
import com.sabbir.arrowpuzzle.ui.theme.ArrowPuzzleTheme

enum class HomeBottomTab {
    PLAY, LEVELS, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: () -> PlayerPrefs,
    onPlay: () -> Unit,
    resumeLabel: String? = null,
    onLevelSelect: (Int) -> Unit = {},
    onChange: ((PlayerPrefs) -> PlayerPrefs) -> Unit = {},
    onReset: () -> Unit = {},
    onUiClick: () -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf(HomeBottomTab.PLAY) }

    // System back inside a secondary tab returns to Play instead of closing.
    BackHandler(enabled = currentTab != HomeBottomTab.PLAY) {
        currentTab = HomeBottomTab.PLAY
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeBottomTab.PLAY,
                    onClick = { onUiClick(); currentTab = HomeBottomTab.PLAY },
                    icon = { PlayIcon(color = if (currentTab == HomeBottomTab.PLAY) GameConfig.Accent else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    label = { Text("PLAY", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameConfig.Accent,
                        indicatorColor = GameConfig.Accent.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == HomeBottomTab.LEVELS,
                    onClick = { onUiClick(); currentTab = HomeBottomTab.LEVELS },
                    icon = { TilesIcon(color = if (currentTab == HomeBottomTab.LEVELS) GameConfig.Accent else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    label = { Text("LEVELS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameConfig.Accent,
                        indicatorColor = GameConfig.Accent.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == HomeBottomTab.SETTINGS,
                    onClick = { onUiClick(); currentTab = HomeBottomTab.SETTINGS },
                    icon = { GearIcon(color = if (currentTab == HomeBottomTab.SETTINGS) GameConfig.Accent else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    label = { Text("SETTINGS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GameConfig.Accent,
                        indicatorColor = GameConfig.Accent.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = currentTab, label = "home_tab_fade") { tab ->
                when (tab) {
                    HomeBottomTab.PLAY -> PlayTabContent(
                        prefs = prefs,
                        onPlay = onPlay,
                        resumeLabel = resumeLabel
                    )
                    HomeBottomTab.LEVELS -> LevelsScreen(
                        prefs = prefs,
                        onBack = { currentTab = HomeBottomTab.PLAY },
                        onLevelSelect = onLevelSelect,
                        showBack = false
                    )
                    HomeBottomTab.SETTINGS -> SettingsScreen(
                        prefs = prefs,
                        onBack = { currentTab = HomeBottomTab.PLAY },
                        onChange = onChange,
                        onReset = onReset,
                        showBack = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayTabContent(
    prefs: () -> PlayerPrefs,
    onPlay: () -> Unit,
    resumeLabel: String?
) {
    val p = prefs()
    val reduced = p.reducedMotion

    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    val floatOffset by if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hero_float"
        )
    }
    val glowPulse by if (reduced) {
        remember { mutableFloatStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )
    }

    val btnOffsetY = remember { androidx.compose.animation.core.Animatable(40f) }
    val btnAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            btnOffsetY.animateTo(0f, tween(300))
            btnAlpha.animateTo(1f, tween(300))
        } else {
            btnOffsetY.snapTo(0f)
            btnAlpha.snapTo(1f)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative glowing ambient circle
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2.2f) * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GameConfig.Accent.copy(alpha = 0.22f),
                        GameConfig.Accent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Master Logo Image
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_logo),
                contentDescription = "Tap Arrows Logo",
                modifier = Modifier
                    .size(104.dp)
                    .offset(y = floatOffset.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = GameConfig.Accent)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Tap Arrows",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = GameConfig.Accent),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameConfig.Accent
                )
            ) {
                Text(
                    text = resumeLabel
                        ?: if (p.unlocked > 1) "CONTINUE LEVEL ${p.unlocked}" else "PLAY GAME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ArrowPuzzleTheme {
        HomeScreen(
            prefs = { PlayerPrefs(unlocked = 2) },
            onPlay = {},
            onLevelSelect = {},
            onChange = {},
            onReset = {}
        )
    }
}
