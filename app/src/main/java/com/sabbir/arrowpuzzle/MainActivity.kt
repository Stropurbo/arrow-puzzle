package com.sabbir.arrowpuzzle

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sabbir.arrowpuzzle.data.PlayerPrefs
import com.sabbir.arrowpuzzle.data.ThemeMode
import com.sabbir.arrowpuzzle.ui.GameplayScreen
import com.sabbir.arrowpuzzle.ui.HomeScreen
import com.sabbir.arrowpuzzle.ui.LevelsScreen
import com.sabbir.arrowpuzzle.ui.SplashScreen
import com.sabbir.arrowpuzzle.ui.theme.ArrowPuzzleTheme
import com.sabbir.arrowpuzzle.vm.GameViewModel

enum class Screen {
    SPLASH, HOME, LEVELS, GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: GameViewModel = viewModel()
            val prefs by vm.prefs.collectAsState()
            val session by vm.session.collectAsState()

            DisposableEffect(lifecycle, vm) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> vm.onAppForeground()
                        Lifecycle.Event.ON_PAUSE -> vm.onAppBackground()
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    vm.onAppForeground()
                }
                onDispose { lifecycle.removeObserver(observer) }
            }

            var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
            val dark = when (prefs.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            // Keep status/navigation bar glyphs legible against the app theme.
            val view = LocalView.current
            SideEffect {
                val window = view.context.findActivity()?.window ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            val resumeLabel = session?.let { "RESUME LEVEL ${it.levelId}" }

            ArrowPuzzleTheme(darkTheme = dark) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn(tween(230)) togetherWith fadeOut(tween(150))
                    },
                    label = "screen_transition"
                ) { target ->
                    when (target) {
                        Screen.SPLASH -> SplashScreen { screen = Screen.HOME }
                        Screen.HOME -> HomeScreen(
                            prefs = { prefs },
                            resumeLabel = resumeLabel,
                            onPlay = {
                                vm.audio.play("click")
                                vm.continueGame(restoreLast = false)
                                screen = Screen.GAME
                            },
                            onLevelSelect = { levelId ->
                                vm.start(levelId)
                                screen = Screen.GAME
                            },
                            onChange = vm::setPrefs,
                            onReset = vm::resetProgress,
                            onUiClick = { vm.audio.play("click") }
                        )
                        Screen.LEVELS -> LevelsScreen(
                            prefs = { prefs },
                            onBack = { screen = Screen.HOME },
                            onLevelSelect = { levelId ->
                                vm.start(levelId)
                                screen = Screen.GAME
                            }
                        )
                        Screen.GAME -> GameplayScreen(
                            vm = vm,
                            onBack = {
                                vm.leaveGame()
                                screen = Screen.HOME
                            },
                            onLevels = {
                                vm.leaveGame()
                                screen = Screen.LEVELS
                            },
                            onNext = {
                                val next = (vm.game.value?.level?.id ?: 1) + 1
                                if (next <= GameConfig.MAX_LEVELS) {
                                    vm.start(next)
                                } else {
                                    vm.leaveGame()
                                    screen = Screen.LEVELS
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true)
@Composable
private fun MainActivityPreview() {
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
