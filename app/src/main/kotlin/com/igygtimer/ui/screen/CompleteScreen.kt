package com.igygtimer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.igygtimer.ui.component.TimerButton
import com.igygtimer.ui.theme.WorkGreen
import com.igygtimer.ui.theme.WorkGreenDark
import com.igygtimer.util.TimeUtils

@Composable
fun CompleteScreen(
    totalTimeMs: Long,
    totalRounds: Int,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkGreenDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "COMPLETE!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "$totalRounds rounds",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${TimeUtils.formatTime(totalTimeMs)} total",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            TimerButton(
                text = "DONE",
                onClick = onDone,
                color = WorkGreen
            )
        }
    }
}
