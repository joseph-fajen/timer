package com.igygtimer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.igygtimer.IGYGApplication
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.ui.component.TimerButton
import com.igygtimer.ui.theme.WorkGreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStartWorkout: (WorkoutConfig) -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember {
        (context.applicationContext as IGYGApplication).container.settingsRepository
    }
    val coroutineScope = rememberCoroutineScope()

    var ratio by remember { mutableStateOf("1.0") }
    var rounds by remember { mutableStateOf("10") }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isLoaded) {
            val settings = settingsRepository.settings.first()
            ratio = settings.lastRatio.toString()
            rounds = settings.lastRounds.toString()
            isLoaded = true
        }
    }

    val ratioPresets = listOf(0.5f, 1.0f, 1.5f, 2.0f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "IGYG Timer",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Work:Rest Ratio",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ratioPresets.forEach { preset ->
                FilterChip(
                    selected = ratio == preset.toString(),
                    onClick = { ratio = preset.toString() },
                    label = { Text("1:$preset") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ratio,
            onValueChange = { ratio = it },
            label = { Text("Custom Ratio") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(200.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Number of Rounds",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(
                onClick = {
                    val current = rounds.toIntOrNull() ?: 10
                    if (current > 1) rounds = (current - 1).toString()
                }
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = rounds,
                onValueChange = { rounds = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp)
            )

            FilledIconButton(
                onClick = {
                    val current = rounds.toIntOrNull() ?: 10
                    rounds = (current + 1).toString()
                }
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        TimerButton(
            text = "START",
            onClick = {
                val ratioValue = ratio.toFloatOrNull() ?: 1.0f
                val roundsValue = rounds.toIntOrNull() ?: 10

                coroutineScope.launch {
                    settingsRepository.saveLastWorkout(ratioValue, roundsValue)
                }

                val config = WorkoutConfig(
                    ratio = ratioValue,
                    totalRounds = roundsValue
                )
                onStartWorkout(config)
            },
            color = WorkGreen
        )
    }
}
