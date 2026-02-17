# Feature: IGYG Timer Phase 1 - Core Timer Implementation

## Feature Description

Native Android timer app for "I Go You Go" workouts. Calculates rest periods dynamically based on actual work time multiplied by a configurable ratio. Phase 1 implements core timer functionality (foreground only, no audio).

## User Story

As a solo IGYG athlete, I want to start a workout with a configurable work:rest ratio so that my rest periods are automatically calculated based on how long I actually worked.

## Solution Statement

Single-module Kotlin/Compose app using MVVM. TimerViewModel manages timer state via StateFlow. Three screens (Home → Timer → Complete) connected via Compose Navigation. Timer uses `SystemClock.uptimeMillis()` for drift-free accuracy.

## Feature Metadata

- **Type**: New Feature (greenfield)
- **Complexity**: Medium
- **Target SDK**: API 36 (Android 16)
- **Min SDK**: API 26 (Android 8.0)

---

## CONTEXT REFERENCES

### Relevant Documentation - READ BEFORE IMPLEMENTING

- [Compose Setup](https://developer.android.com/develop/ui/compose/setup) - Project creation
- [ViewModel with Compose](https://developer.android.com/develop/ui/compose/state#viewmodel-state) - State management pattern
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation) - Screen navigation
- [SystemClock.uptimeMillis](https://developer.android.com/reference/android/os/SystemClock#uptimeMillis()) - Monotonic time source

### Project Structure

```
app/src/main/kotlin/com/igygtimer/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   ├── screen/
│   │   ├── HomeScreen.kt
│   │   ├── TimerScreen.kt
│   │   └── CompleteScreen.kt
│   └── component/
│       ├── TimeDisplay.kt
│       ├── RoundIndicator.kt
│       └── TimerButton.kt
├── viewmodel/
│   └── TimerViewModel.kt
├── model/
│   ├── TimerState.kt
│   └── WorkoutConfig.kt
└── navigation/
    └── NavGraph.kt
```

### Key Patterns

**State Exposure (ViewModel):**
```kotlin
private val _uiState = MutableStateFlow(TimerUiState())
val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
```

**Timer Tick Loop:**
```kotlin
viewModelScope.launch {
    while (isActive && phase is Running) {
        val elapsed = SystemClock.uptimeMillis() - phaseStartTime
        _uiState.update { it.copy(elapsedMs = elapsed) }
        delay(50) // 20 FPS update
    }
}
```

**Time Formatting:**
```kotlin
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
```

**Keep Screen On:**
```kotlin
DisposableEffect(Unit) {
    val window = (context as Activity).window
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
}
```

---

## STATE MACHINE

```
IDLE ──[start]──► WORK ──[done tap]──► REST ──[countdown=0]──► WORK ──...
                    │                    │                        │
                    ▼                    ▼                        ▼
                 PAUSED ◄────────────────┴────────────────────────┘
                    │
                    ▼
              [stop tap] ──► IDLE

Final round: REST ──[countdown=0]──► COMPLETE
```

**State Model:**
```kotlin
sealed class TimerPhase {
    object Idle : TimerPhase()
    data class Work(val round: Int) : TimerPhase()
    data class Rest(val round: Int, val restDurationMs: Long) : TimerPhase()
    data class Paused(val previousPhase: TimerPhase, val remainingMs: Long) : TimerPhase()
    object Complete : TimerPhase()
}

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.Idle,
    val elapsedMs: Long = 0,
    val totalRounds: Int = 10,
    val ratio: Float = 1.0f
)

data class WorkoutConfig(
    val ratio: Float,
    val totalRounds: Int
)
```

---

## IMPLEMENTATION PLAN

### Phase 1: Project Setup

1. Create new Android Studio project (Empty Activity - Compose)
2. Configure build.gradle.kts with dependencies
3. Set up package structure

### Phase 2: Core Models & ViewModel

1. Create state models (TimerPhase, TimerUiState, WorkoutConfig)
2. Implement TimerViewModel with timer logic
3. Add time formatting utility

### Phase 3: UI Components

1. Create theme (colors, typography)
2. Build reusable components (TimeDisplay, RoundIndicator, TimerButton)
3. Implement screens (Home, Timer, Complete)

### Phase 4: Navigation & Integration

1. Set up NavGraph with routes
2. Wire MainActivity
3. Add keep-screen-on behavior

---

## STEP-BY-STEP TASKS

### Task 1: CREATE Android Studio Project

- Open Android Studio → New Project → Empty Activity (Compose)
- Name: `IGYGTimer`
- Package: `com.igygtimer`
- Min SDK: API 26
- Language: Kotlin
- **VALIDATE**: Project syncs and builds without errors

### Task 2: UPDATE `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.igygtimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.igygtimer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Add these
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

- **VALIDATE**: `./gradlew build` succeeds

### Task 3: CREATE `model/WorkoutConfig.kt`

```kotlin
package com.igygtimer.model

data class WorkoutConfig(
    val ratio: Float,
    val totalRounds: Int
)
```

### Task 4: CREATE `model/TimerState.kt`

```kotlin
package com.igygtimer.model

sealed class TimerPhase {
    object Idle : TimerPhase()
    data class Work(val round: Int) : TimerPhase()
    data class Rest(val round: Int, val durationMs: Long) : TimerPhase()
    data class Paused(val from: TimerPhase, val remainingMs: Long) : TimerPhase()
    object Complete : TimerPhase()
}

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.Idle,
    val displayTimeMs: Long = 0,
    val currentRound: Int = 1,
    val totalRounds: Int = 10,
    val ratio: Float = 1.0f,
    val totalElapsedMs: Long = 0
)
```

### Task 5: CREATE `viewmodel/TimerViewModel.kt`

```kotlin
package com.igygtimer.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var phaseStartTime: Long = 0
    private var workStartTime: Long = 0
    private var lastWorkDurationMs: Long = 0
    private var workoutStartTime: Long = 0

    fun startWorkout(config: WorkoutConfig) {
        _uiState.update {
            it.copy(
                totalRounds = config.totalRounds,
                ratio = config.ratio,
                currentRound = 1,
                totalElapsedMs = 0
            )
        }
        workoutStartTime = SystemClock.uptimeMillis()
        startWork()
    }

    private fun startWork() {
        phaseStartTime = SystemClock.uptimeMillis()
        workStartTime = phaseStartTime
        _uiState.update { it.copy(phase = TimerPhase.Work(it.currentRound), displayTimeMs = 0) }
        startTickLoop()
    }

    fun onWorkDone() {
        val now = SystemClock.uptimeMillis()
        lastWorkDurationMs = now - workStartTime
        val restDurationMs = (lastWorkDurationMs * _uiState.value.ratio).toLong()
        startRest(restDurationMs)
    }

    private fun startRest(durationMs: Long) {
        timerJob?.cancel()
        phaseStartTime = SystemClock.uptimeMillis()
        val round = _uiState.value.currentRound
        _uiState.update { it.copy(phase = TimerPhase.Rest(round, durationMs), displayTimeMs = durationMs) }
        startRestCountdown(durationMs)
    }

    private fun startRestCountdown(durationMs: Long) {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = SystemClock.uptimeMillis() - phaseStartTime
                val remaining = (durationMs - elapsed).coerceAtLeast(0)
                val totalElapsed = SystemClock.uptimeMillis() - workoutStartTime
                _uiState.update { it.copy(displayTimeMs = remaining, totalElapsedMs = totalElapsed) }

                if (remaining <= 0) {
                    onRestComplete()
                    break
                }
                delay(50)
            }
        }
    }

    private fun onRestComplete() {
        val state = _uiState.value
        if (state.currentRound >= state.totalRounds) {
            _uiState.update { it.copy(phase = TimerPhase.Complete) }
        } else {
            _uiState.update { it.copy(currentRound = it.currentRound + 1) }
            startWork()
        }
    }

    private fun startTickLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = SystemClock.uptimeMillis() - phaseStartTime
                val totalElapsed = SystemClock.uptimeMillis() - workoutStartTime
                _uiState.update { it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed) }
                delay(50)
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        val state = _uiState.value
        val remaining = when (val phase = state.phase) {
            is TimerPhase.Work -> state.displayTimeMs
            is TimerPhase.Rest -> state.displayTimeMs
            else -> 0
        }
        _uiState.update { it.copy(phase = TimerPhase.Paused(state.phase, remaining)) }
    }

    fun resume() {
        val state = _uiState.value
        val paused = state.phase as? TimerPhase.Paused ?: return
        phaseStartTime = SystemClock.uptimeMillis()

        when (val from = paused.from) {
            is TimerPhase.Work -> {
                val alreadyElapsed = paused.remainingMs
                _uiState.update { it.copy(phase = from) }
                timerJob = viewModelScope.launch {
                    while (isActive) {
                        val elapsed = alreadyElapsed + (SystemClock.uptimeMillis() - phaseStartTime)
                        val totalElapsed = SystemClock.uptimeMillis() - workoutStartTime
                        _uiState.update { it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed) }
                        delay(50)
                    }
                }
            }
            is TimerPhase.Rest -> {
                val remaining = paused.remainingMs
                _uiState.update { it.copy(phase = from, displayTimeMs = remaining) }
                startRestCountdown(remaining)
            }
            else -> {}
        }
    }

    fun stop() {
        timerJob?.cancel()
        _uiState.update { TimerUiState() }
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.update { TimerUiState() }
    }
}
```

- **VALIDATE**: File compiles without errors

### Task 6: CREATE `ui/theme/Color.kt`

```kotlin
package com.igygtimer.ui.theme

import androidx.compose.ui.graphics.Color

val WorkGreen = Color(0xFF2E7D32)
val WorkGreenDark = Color(0xFF1B5E20)
val RestBlue = Color(0xFF1976D2)
val RestBlueDark = Color(0xFF0D47A1)
val PausedOrange = Color(0xFFE65100)
val BackgroundDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFFAFAFA)
val TextOnDark = Color(0xFFFFFFFF)
val TextOnLight = Color(0xFF000000)
```

### Task 7: CREATE `ui/theme/Type.kt`

```kotlin
package com.igygtimer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 104.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp,
        lineHeight = 68.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)
```

### Task 8: UPDATE `ui/theme/Theme.kt`

```kotlin
package com.igygtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WorkGreen,
    secondary = RestBlue,
    tertiary = PausedOrange,
    background = BackgroundDark,
    surface = BackgroundDark,
    onPrimary = TextOnDark,
    onSecondary = TextOnDark,
    onBackground = TextOnDark,
    onSurface = TextOnDark
)

@Composable
fun IGYGTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
```

### Task 9: CREATE `ui/component/TimeDisplay.kt`

```kotlin
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
```

### Task 10: CREATE `ui/component/RoundIndicator.kt`

```kotlin
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
```

### Task 11: CREATE `ui/component/TimerButton.kt`

```kotlin
package com.igygtimer.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TimerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(80.dp)
            .width(200.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
```

### Task 12: CREATE `ui/screen/HomeScreen.kt`

```kotlin
package com.igygtimer.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.ui.component.TimerButton
import com.igygtimer.ui.theme.WorkGreen

@Composable
fun HomeScreen(
    onStartWorkout: (WorkoutConfig) -> Unit
) {
    var ratio by remember { mutableStateOf("1.0") }
    var rounds by remember { mutableStateOf("10") }

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

        // Ratio presets
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

        // Rounds
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
                val config = WorkoutConfig(
                    ratio = ratio.toFloatOrNull() ?: 1.0f,
                    totalRounds = rounds.toIntOrNull() ?: 10
                )
                onStartWorkout(config)
            },
            color = WorkGreen
        )
    }
}
```

### Task 13: CREATE `ui/screen/TimerScreen.kt`

```kotlin
package com.igygtimer.ui.screen

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.igygtimer.ui.theme.*

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
```

### Task 14: CREATE `ui/screen/CompleteScreen.kt`

```kotlin
package com.igygtimer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.igygtimer.ui.component.TimerButton
import com.igygtimer.ui.theme.WorkGreen

@Composable
fun CompleteScreen(
    totalTimeMs: Long,
    totalRounds: Int,
    onDone: () -> Unit
) {
    val minutes = totalTimeMs / 1000 / 60
    val seconds = (totalTimeMs / 1000) % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20)),
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
                text = String.format("%d:%02d total", minutes, seconds),
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
```

### Task 15: CREATE `navigation/NavGraph.kt`

```kotlin
package com.igygtimer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.igygtimer.ui.screen.CompleteScreen
import com.igygtimer.ui.screen.HomeScreen
import com.igygtimer.ui.screen.TimerScreen
import com.igygtimer.viewmodel.TimerViewModel

object Routes {
    const val HOME = "home"
    const val TIMER = "timer"
    const val COMPLETE = "complete"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: TimerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = { config ->
                    viewModel.startWorkout(config)
                    navController.navigate(Routes.TIMER)
                }
            )
        }

        composable(Routes.TIMER) {
            TimerScreen(
                uiState = uiState,
                onWorkDone = { viewModel.onWorkDone() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onStop = {
                    viewModel.stop()
                    navController.popBackStack(Routes.HOME, false)
                },
                onComplete = {
                    navController.navigate(Routes.COMPLETE) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.COMPLETE) {
            CompleteScreen(
                totalTimeMs = uiState.totalElapsedMs,
                totalRounds = uiState.totalRounds,
                onDone = {
                    viewModel.reset()
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}
```

### Task 16: UPDATE `MainActivity.kt`

```kotlin
package com.igygtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.igygtimer.navigation.NavGraph
import com.igygtimer.ui.theme.IGYGTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IGYGTimerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
```

- **VALIDATE**: `./gradlew build` succeeds

### Task 17: Test on Device/Emulator

1. Connect Pixel 7 Pro or launch API 36 emulator
2. Run app from Android Studio (Shift+F10)
3. Test flow: Set ratio 1:1, 3 rounds → Start → Work (tap) → Rest countdown → repeat → Complete
- **VALIDATE**: All phase transitions work correctly, timer is accurate

### Task 18: Generate Signed APK

1. Build → Generate Signed Bundle/APK → APK
2. Create new keystore (first time): `igyg-release-key.jks`
3. Select release build variant
4. Build
- **VALIDATE**: APK exists at `app/build/outputs/apk/release/app-release.apk`

---

## VALIDATION COMMANDS

```bash
# Build
./gradlew build

# Run lint
./gradlew lint

# Install on connected device
./gradlew installDebug

# Generate release APK
./gradlew assembleRelease
```

---

## ACCEPTANCE CRITERIA

- [ ] App launches on Pixel 7 Pro / Pixel 9a (Android 16)
- [ ] Home screen allows ratio selection (presets + custom) and round count
- [ ] Timer starts in WORK phase, counts UP
- [ ] Tapping during WORK transitions to REST with correct duration (work × ratio)
- [ ] REST counts DOWN and auto-transitions to next WORK
- [ ] Final REST auto-transitions to COMPLETE screen
- [ ] Pause/Resume works from WORK and REST phases
- [ ] Stop shows confirmation dialog, returns to Home
- [ ] Screen stays on during timer
- [ ] Signed APK can be installed via sideload

---

## NOTES

**Phase 1 Limitations (by design):**
- App must stay in foreground (no background service)
- No audio cues (Phase 2)
- No settings persistence (Phase 2)
- No dark/light theme toggle (uses dark only)

**Future Phase 2 additions:**
- Foreground service for background execution
- Audio beeps at 3, 2, 1 seconds
- Settings persistence (SharedPreferences)
- Final round distinct audio

**Keystore Security:**
- Store `igyg-release-key.jks` securely (not in git)
- Add `keystore.properties` to `.gitignore`
