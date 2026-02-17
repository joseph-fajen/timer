package com.igygtimer.ui.screen

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import com.igygtimer.ui.component.RoundIndicator
import com.igygtimer.ui.component.TimeDisplay
import com.igygtimer.ui.component.TimerButton
import com.igygtimer.ui.theme.BackgroundDark
import com.igygtimer.ui.theme.PausedOrange
import com.igygtimer.ui.theme.RestBlue
import com.igygtimer.ui.theme.WorkGreen

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onWorkDone: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    // Keep screen on
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-navigate on complete
    LaunchedEffect(uiState.phase) {
        if (uiState.phase is TimerPhase.Complete) {
            onComplete()
        }
    }

    val backgroundColor = when (uiState.phase) {
        is TimerPhase.Work -> WorkGreen
        is TimerPhase.Rest -> RestBlue
        is TimerPhase.Paused -> PausedOrange
        else -> BackgroundDark
    }

    val phaseLabel = when (uiState.phase) {
        is TimerPhase.Work -> "WORK"
        is TimerPhase.Rest -> "REST"
        is TimerPhase.Paused -> "PAUSED"
        else -> ""
    }

    val isPaused = uiState.phase is TimerPhase.Paused
    val isWork = uiState.phase is TimerPhase.Work

    var showStopDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (isWork) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onWorkDone() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RoundIndicator(
                currentRound = uiState.currentRound,
                totalRounds = uiState.totalRounds
            )

            Spacer(modifier = Modifier.height(24.dp))

            TimeDisplay(timeMs = uiState.displayTimeMs)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            if (isWork) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Tap anywhere when done",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimerButton(
                    text = if (isPaused) "RESUME" else "PAUSE",
                    onClick = { if (isPaused) onResume() else onPause() },
                    color = Color.White.copy(alpha = 0.3f)
                )

                TimerButton(
                    text = "STOP",
                    onClick = { showStopDialog = true },
                    color = Color.Red.copy(alpha = 0.7f)
                )
            }
        }

        // Total time at bottom
        Text(
            text = "Total: ${formatTotalTime(uiState.totalElapsedMs)}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("End Workout?") },
            text = { Text("You're on round ${uiState.currentRound} of ${uiState.totalRounds}") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onStop()
                }) {
                    Text("END")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

private fun formatTotalTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
