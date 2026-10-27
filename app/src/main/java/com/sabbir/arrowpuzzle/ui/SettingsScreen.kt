package com.sabbir.arrowpuzzle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sabbir.arrowpuzzle.BuildConfig
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.data.PlayerPrefs
import com.sabbir.arrowpuzzle.ui.theme.ArrowPuzzleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: () -> PlayerPrefs,
    onBack: () -> Unit,
    onChange: ((PlayerPrefs) -> PlayerPrefs) -> Unit,
    onReset: () -> Unit,
    showBack: Boolean = true
) {
    val p = prefs()
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (showBack) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Offline • no ads • no tracking",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SectionLabel("Audio & feedback")
            FlatCard {
                ToggleRow(
                    title = "Music",
                    subtitle = "Calm looping soundtrack",
                    checked = p.music
                ) { value -> onChange { it.copy(music = value) } }
                RowDivider()
                ToggleRow(
                    title = "Sound effects",
                    subtitle = "Taps, exits and heart loss",
                    checked = p.sound
                ) { value -> onChange { it.copy(sound = value) } }
                RowDivider()
                ToggleRow(
                    title = "Haptics",
                    subtitle = "A light tick on each tap",
                    checked = p.haptics
                ) { value -> onChange { it.copy(haptics = value) } }
            }

            Spacer(Modifier.height(8.dp))

            SectionLabel("Accessibility")
            FlatCard {
                ToggleRow(
                    title = "Reduced motion",
                    subtitle = "Skip shake, confetti and slide",
                    checked = p.reducedMotion
                ) { value -> onChange { it.copy(reducedMotion = value) } }
                RowDivider()
                ToggleRow(
                    title = "High contrast",
                    subtitle = "Thicker, darker arrows",
                    checked = p.highContrast
                ) { value -> onChange { it.copy(highContrast = value) } }
                RowDivider()
                ToggleRow(
                    title = "Larger tap targets",
                    subtitle = "Forgiving hits on dense boards",
                    checked = p.largeHitArea
                ) { value -> onChange { it.copy(largeHitArea = value) } }
            }

            Spacer(Modifier.height(8.dp))

            SectionLabel("Appearance")
            ThemeSelector(selected = p.theme) { mode ->
                onChange { it.copy(theme = mode) }
            }

            Spacer(Modifier.height(8.dp))

            SectionLabel("About")
            FlatCard {
                InfoRow("Version", BuildConfig.VERSION_NAME)
                RowDivider()
                InfoRow("Levels solved", "${p.completed.size} / ${GameConfig.MAX_LEVELS}")
                RowDivider()
                Text(
                    text = "Everything lives on this device. No accounts, no analytics and nothing is uploaded.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GameConfig.Danger.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GameConfig.Danger
                )
            ) {
                TrashIcon(color = GameConfig.Danger, size = 15.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "RESET PROGRESS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        val stars = p.bestStars.values.sum()
        ConfirmDialog(
            title = "Reset progress?",
            message = "This clears $stars stars across ${p.completed.size} solved levels, your best times, daily streak, trophies and saved settings. It cannot be undone.",
            confirmLabel = "RESET",
            destructive = true,
            icon = { TrashIcon(color = GameConfig.Danger, size = 28.dp) },
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ArrowPuzzleTheme {
        SettingsScreen(
            prefs = { PlayerPrefs() },
            onBack = {},
            onChange = {},
            onReset = {}
        )
    }
}
