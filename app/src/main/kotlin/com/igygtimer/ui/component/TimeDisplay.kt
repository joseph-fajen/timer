package com.igygtimer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun TimeDisplay(
    timeMs: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeString = String.format("%d:%02d", minutes, seconds)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge,
            color = color
        )
    }
}
