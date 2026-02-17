package com.igygtimer.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun RoundIndicator(
    currentRound: Int,
    totalRounds: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Text(
        text = "Round $currentRound of $totalRounds",
        style = MaterialTheme.typography.headlineLarge,
        color = color.copy(alpha = 0.8f),
        modifier = modifier
    )
}
