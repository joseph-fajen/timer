package com.igygtimer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.igygtimer.util.TimeUtils

@Composable
fun TimeDisplay(
    timeMs: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val timeString = TimeUtils.formatTime(timeMs)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge,
            color = color
        )
    }
}
