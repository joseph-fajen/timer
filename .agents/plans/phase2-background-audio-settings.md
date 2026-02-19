# Feature: Phase 2 - Background Service, Audio Beeps, and Settings Persistence

The following plan should be complete, but validate documentation and codebase patterns before implementing.

Pay special attention to naming of existing utils, types, and models. Import from the right files.

## Feature Description

Transform the IGYG Timer from a foreground-only app to a fully background-capable workout timer with audio countdown beeps and persistent settings. This involves:

1. **Background Service Execution** - Foreground service with wake lock so timer continues when screen off
2. **Audio Beeps** - 3, 2, 1 countdown beeps during rest phase using SoundPool
3. **Settings Persistence** - Save last-used ratio and rounds using Jetpack DataStore

The architecture uses a **TimerRepository as single source of truth** - both the foreground service and ViewModel observe/update through this shared repository.

## User Story

As a solo IGYG athlete
I want the timer to work reliably with my phone screen off and hear countdown beeps
So that I can complete my workout without watching my phone constantly

## Problem Statement

The current Phase 1 implementation only works in the foreground. When the user locks their phone or switches apps, the timer stops because it runs in `viewModelScope` which is tied to the UI lifecycle. Users need audio cues to know when rest is ending without looking at the screen.

## Solution Statement

Extract timer logic from ViewModel into a shared TimerRepository. Create a foreground service that runs the tick loop with a wake lock, plays countdown beeps, and shows a persistent notification. Use DataStore to remember the user's last workout configuration.

## Feature Metadata

**Feature Type**: Enhancement (Phase 2 of existing app)
**Estimated Complexity**: High
**Primary Systems Affected**: TimerViewModel, AndroidManifest, HomeScreen, Navigation
**Dependencies**: Jetpack DataStore, AndroidX LifecycleService

---

## CONTEXT REFERENCES

### Relevant Codebase Files - IMPORTANT: READ THESE FILES BEFORE IMPLEMENTING!

| File | Lines | Why Read This |
|------|-------|---------------|
| `viewmodel/TimerViewModel.kt` | 1-152 | Contains timer logic to extract to repository |
| `model/TimerState.kt` | 1-18 | TimerPhase sealed class and TimerUiState - no changes needed |
| `util/TimeProvider.kt` | 1-20 | Time abstraction pattern to reuse in repository |
| `test/util/FakeTimeProvider.kt` | 1-20 | Test fake pattern to follow for new components |
| `ui/screen/HomeScreen.kt` | 30-138 | Needs settings integration (lines 34-35 use local state) |
| `navigation/NavGraph.kt` | 1-67 | Creates ViewModel, needs service start/stop |
| `MainActivity.kt` | 1-27 | Entry point, may need service binding |
| `AndroidManifest.xml` | 1-28 | Add permissions and service declaration |
| `gradle/libs.versions.toml` | 1-34 | Add DataStore dependency here |
| `app/build.gradle.kts` | 1-63 | Reference new dependencies |

### New Files to Create

```
app/src/main/kotlin/com/igygtimer/
├── IGYGApplication.kt              # Custom Application with AppContainer
├── di/
│   └── AppContainer.kt             # Manual DI container
├── repository/
│   ├── TimerRepository.kt          # Timer state + logic (extracted from ViewModel)
│   └── SettingsRepository.kt       # DataStore wrapper
├── service/
│   └── TimerService.kt             # Foreground service with wake lock
└── audio/
    └── BeepPlayer.kt               # SoundPool wrapper

app/src/main/res/raw/
└── beep.ogg                        # Countdown beep sound file

app/src/test/kotlin/com/igygtimer/
└── repository/
    └── TimerRepositoryTest.kt      # Tests for repository
```

### Relevant Documentation - READ THESE BEFORE IMPLEMENTING!

- [Foreground Services Overview](https://developer.android.com/develop/background-work/services/fgs)
  - Section: Creating a foreground service
  - Why: Core pattern for TimerService

- [Foreground Service Types](https://developer.android.com/develop/background-work/services/fgs/service-types)
  - Section: specialUse type
  - Why: Required for workout timer on API 34+

- [DataStore Guide](https://developer.android.com/topic/libraries/architecture/datastore)
  - Section: Preferences DataStore
  - Why: SettingsRepository implementation

- [SoundPool Reference](https://developer.android.com/reference/kotlin/android/media/SoundPool)
  - Section: Builder pattern
  - Why: BeepPlayer implementation

- [Manual Dependency Injection](https://developer.android.com/training/dependency-injection/manual)
  - Section: Managing dependencies with a container
  - Why: AppContainer pattern

### Patterns to Follow

**State Exposure (from TimerViewModel.kt:22-23):**
```kotlin
private val _uiState = MutableStateFlow(TimerUiState())
val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
```

**Time Provider Abstraction (from TimeProvider.kt:10-12):**
```kotlin
interface TimeProvider {
    fun uptimeMillis(): Long
}
```

**Timer Tick Loop (from TimerViewModel.kt:93-102):**
```kotlin
timerJob = viewModelScope.launch {
    while (isActive) {
        val elapsed = timeProvider.uptimeMillis() - phaseStartTime
        _uiState.update { it.copy(displayTimeMs = elapsed) }
        delay(50)
    }
}
```

**Test Fake Pattern (from FakeTimeProvider.kt):**
```kotlin
class FakeTimeProvider(private var currentTimeMs: Long = 0L) : TimeProvider {
    override fun uptimeMillis(): Long = currentTimeMs
    fun advanceBy(millis: Long) { currentTimeMs += millis }
}
```

---

## ARCHITECTURE

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      IGYGApplication                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                     AppContainer                             │ │
│  │  ┌─────────────────┐  ┌──────────────────┐                  │ │
│  │  │ TimerRepository │  │SettingsRepository│                  │ │
│  │  │  (StateFlow)    │  │   (DataStore)    │                  │ │
│  │  └────────┬────────┘  └──────────────────┘                  │ │
│  └───────────┼──────────────────────────────────────────────────┘ │
└──────────────┼──────────────────────────────────────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
     ▼                   ▼
┌─────────────┐   ┌──────────────┐
│TimerService │   │TimerViewModel│
│ - tick loop │   │ - observe    │
│ - wake lock │   │ - UI events  │
│ - beeps     │   └──────────────┘
│ - notif     │          │
└─────────────┘          ▼
                  ┌──────────────┐
                  │   Compose    │
                  │   Screens    │
                  └──────────────┘
```

### Data Flow

1. **Start Workout**: UI → ViewModel → Repository.startWorkout() → Service started
2. **Timer Tick**: Service → Repository._uiState.update() → ViewModel observes → UI updates
3. **Beep Trigger**: Repository detects countdown 3/2/1 → calls BeepPlayer
4. **Work Done**: UI → ViewModel → Repository.onWorkDone()
5. **Stop Workout**: UI → ViewModel → Repository.stop() → Service stopped

---

## IMPLEMENTATION PLAN

### Phase 1: Dependencies and Configuration

1. Add DataStore to version catalog and build.gradle
2. Add permissions and service declaration to AndroidManifest
3. Create beep sound asset

### Phase 2: Core Infrastructure

1. Create AppContainer for manual DI
2. Create IGYGApplication to hold container
3. Create SettingsRepository with DataStore

### Phase 3: Timer Repository

1. Extract timer logic from ViewModel to TimerRepository
2. Refactor TimerViewModel to observe repository
3. Add tests for TimerRepository

### Phase 4: Audio System

1. Create BeepPlayer with SoundPool
2. Integrate beep triggers into TimerRepository

### Phase 5: Foreground Service

1. Create TimerService with notification
2. Add wake lock handling
3. Wire service start/stop to navigation

### Phase 6: Settings Integration

1. Update HomeScreen to load saved settings
2. Save settings when workout starts

---

## STEP-BY-STEP TASKS

### Task 1: UPDATE `gradle/libs.versions.toml` - Add DataStore

**IMPLEMENT**: Add DataStore dependency to version catalog

```toml
# Add to [versions] section
datastore = "1.1.1"

# Add to [libraries] section
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

**PATTERN**: Follow existing dependency format (see lines 14-29)
**VALIDATE**: `./gradlew build` succeeds

---

### Task 2: UPDATE `app/build.gradle.kts` - Reference DataStore

**IMPLEMENT**: Add DataStore to dependencies block

```kotlin
// Add after line 54 (navigation-compose)
implementation(libs.androidx.datastore.preferences)
```

**PATTERN**: Follow existing `implementation(libs.xxx)` pattern (lines 43-54)
**VALIDATE**: `./gradlew build` succeeds

---

### Task 3: UPDATE `AndroidManifest.xml` - Add Permissions and Service

**IMPLEMENT**: Add required permissions and service declaration

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Add these permissions before <application> -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".IGYGApplication"
        android:allowBackup="true"
        ... existing attributes ...
        tools:targetApi="31">

        <!-- Existing activity -->
        <activity ... />

        <!-- Add service declaration -->
        <service
            android:name=".service.TimerService"
            android:foregroundServiceType="specialUse"
            android:exported="false">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Workout timer requiring continuous timing and audio cues during exercise"/>
        </service>
    </application>
</manifest>
```

**GOTCHA**: Must add `android:name=".IGYGApplication"` to application tag
**VALIDATE**: `./gradlew build` succeeds (will fail until IGYGApplication exists)

---

### Task 4: CREATE `res/raw/beep.ogg` - Sound Asset

**IMPLEMENT**: Create or download a short beep sound file

Option A: Generate programmatically (recommended for simplicity)
- Use an online tone generator to create a 440Hz sine wave, 150ms duration
- Export as OGG format (smaller than WAV, well-supported)
- Save to `app/src/main/res/raw/beep.ogg`

Option B: Use Android system sound
- Can use `RingtoneManager.TYPE_NOTIFICATION` as fallback

**GOTCHA**: File must be lowercase, no special characters
**GOTCHA**: Keep file < 50KB to stay under SoundPool limits
**VALIDATE**: File exists at `app/src/main/res/raw/beep.ogg`

---

### Task 5: CREATE `di/AppContainer.kt` - Manual DI Container

**IMPLEMENT**: Create container class to hold repository singletons

```kotlin
package com.igygtimer.di

import android.content.Context
import com.igygtimer.repository.SettingsRepository
import com.igygtimer.repository.TimerRepository
import com.igygtimer.util.SystemTimeProvider

/**
 * Manual dependency injection container.
 * Holds singleton instances of repositories shared across app components.
 */
class AppContainer(context: Context) {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val timerRepository: TimerRepository by lazy {
        TimerRepository(timeProvider = SystemTimeProvider())
    }
}
```

**PATTERN**: Lazy initialization for on-demand creation
**IMPORTS**: SettingsRepository, TimerRepository, SystemTimeProvider
**VALIDATE**: File compiles (will fail until repositories exist)

---

### Task 6: CREATE `IGYGApplication.kt` - Custom Application Class

**IMPLEMENT**: Create Application subclass to hold AppContainer

```kotlin
package com.igygtimer

import android.app.Application
import com.igygtimer.di.AppContainer

class IGYGApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

**PATTERN**: Standard Application subclass pattern
**GOTCHA**: Must be declared in AndroidManifest (Task 3)
**VALIDATE**: `./gradlew build` succeeds (after repositories exist)

---

### Task 7: CREATE `repository/SettingsRepository.kt` - DataStore Wrapper

**IMPLEMENT**: Create repository for persisting workout settings

```kotlin
package com.igygtimer.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore singleton - created once per process
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

/**
 * User's last workout configuration.
 */
data class UserSettings(
    val lastRatio: Float = 1.0f,
    val lastRounds: Int = 10
)

/**
 * Repository for persisting user settings via DataStore.
 */
class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val LAST_RATIO = floatPreferencesKey("last_ratio")
        val LAST_ROUNDS = intPreferencesKey("last_rounds")
    }

    /**
     * Flow of current settings. Emits new value when settings change.
     */
    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            lastRatio = prefs[PreferenceKeys.LAST_RATIO] ?: 1.0f,
            lastRounds = prefs[PreferenceKeys.LAST_ROUNDS] ?: 10
        )
    }

    /**
     * Save the last used workout configuration.
     */
    suspend fun saveLastWorkout(ratio: Float, rounds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_RATIO] = ratio
            prefs[PreferenceKeys.LAST_ROUNDS] = rounds
        }
    }
}
```

**PATTERN**: Top-level `preferencesDataStore` extension for singleton
**IMPORTS**: DataStore, Preferences, edit, Flow, map
**VALIDATE**: `./gradlew build` succeeds

---

### Task 8: CREATE `repository/TimerRepository.kt` - Timer State and Logic

**IMPLEMENT**: Extract timer logic from ViewModel into repository

```kotlin
package com.igygtimer.repository

import com.igygtimer.audio.BeepPlayer
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Repository holding timer state and logic.
 * Shared between TimerService (runs tick loop) and TimerViewModel (observes state).
 */
class TimerRepository(
    private val timeProvider: TimeProvider
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    // Timing bookkeeping
    private var phaseStartTime: Long = 0
    private var workStartTime: Long = 0
    private var workoutStartTime: Long = 0
    private var pausedElapsedWork: Long = 0

    // Beep tracking to avoid duplicate beeps
    private var lastBeepSecond: Int = -1

    // BeepPlayer injected when service starts
    var beepPlayer: BeepPlayer? = null

    /**
     * Start a new workout with the given configuration.
     * Called from ViewModel, triggers service start externally.
     */
    fun startWorkout(config: WorkoutConfig) {
        lastBeepSecond = -1
        _uiState.update {
            TimerUiState(
                phase = TimerPhase.Idle,
                totalRounds = config.totalRounds,
                ratio = config.ratio,
                currentRound = 1,
                totalElapsedMs = 0
            )
        }
        workoutStartTime = timeProvider.uptimeMillis()
        startWork()
    }

    /**
     * Transition to work phase.
     */
    private fun startWork() {
        phaseStartTime = timeProvider.uptimeMillis()
        workStartTime = phaseStartTime
        pausedElapsedWork = 0
        _uiState.update {
            it.copy(
                phase = TimerPhase.Work(it.currentRound),
                displayTimeMs = 0
            )
        }
    }

    /**
     * Called when user taps "Done" during work phase.
     */
    fun onWorkDone() {
        val state = _uiState.value
        if (state.phase !is TimerPhase.Work) return

        val workDuration = if (pausedElapsedWork > 0) {
            pausedElapsedWork + (timeProvider.uptimeMillis() - phaseStartTime)
        } else {
            timeProvider.uptimeMillis() - workStartTime
        }

        val restDurationMs = (workDuration * state.ratio).toLong()
        startRest(restDurationMs)
    }

    /**
     * Transition to rest phase with calculated duration.
     */
    private fun startRest(durationMs: Long) {
        phaseStartTime = timeProvider.uptimeMillis()
        lastBeepSecond = -1
        val round = _uiState.value.currentRound
        _uiState.update {
            it.copy(
                phase = TimerPhase.Rest(round, durationMs),
                displayTimeMs = durationMs
            )
        }
    }

    /**
     * Called by service on each tick (every 50ms).
     * Updates state and triggers beeps when appropriate.
     */
    fun tick() {
        val state = _uiState.value
        val now = timeProvider.uptimeMillis()
        val totalElapsed = now - workoutStartTime

        when (val phase = state.phase) {
            is TimerPhase.Work -> {
                val elapsed = if (pausedElapsedWork > 0) {
                    pausedElapsedWork + (now - phaseStartTime)
                } else {
                    now - phaseStartTime
                }
                _uiState.update {
                    it.copy(displayTimeMs = elapsed, totalElapsedMs = totalElapsed)
                }
            }
            is TimerPhase.Rest -> {
                val elapsed = now - phaseStartTime
                val remaining = (phase.durationMs - elapsed).coerceAtLeast(0)
                _uiState.update {
                    it.copy(displayTimeMs = remaining, totalElapsedMs = totalElapsed)
                }

                // Trigger beeps at 3, 2, 1 seconds
                val remainingSeconds = (remaining / 1000).toInt()
                if (remainingSeconds in 1..3 && remainingSeconds != lastBeepSecond) {
                    lastBeepSecond = remainingSeconds
                    beepPlayer?.playBeep()
                }

                // Transition when rest complete
                if (remaining <= 0) {
                    onRestComplete()
                }
            }
            else -> { /* No tick needed for Idle, Paused, Complete */ }
        }
    }

    /**
     * Called when rest countdown reaches zero.
     */
    private fun onRestComplete() {
        val state = _uiState.value
        if (state.currentRound >= state.totalRounds) {
            _uiState.update { it.copy(phase = TimerPhase.Complete) }
        } else {
            _uiState.update { it.copy(currentRound = it.currentRound + 1) }
            startWork()
        }
    }

    /**
     * Pause the timer (from work or rest phase).
     */
    fun pause() {
        val state = _uiState.value
        val phase = state.phase

        when (phase) {
            is TimerPhase.Work -> {
                val elapsed = if (pausedElapsedWork > 0) {
                    pausedElapsedWork + (timeProvider.uptimeMillis() - phaseStartTime)
                } else {
                    timeProvider.uptimeMillis() - phaseStartTime
                }
                _uiState.update {
                    it.copy(phase = TimerPhase.Paused(phase, elapsed))
                }
            }
            is TimerPhase.Rest -> {
                val remaining = state.displayTimeMs
                _uiState.update {
                    it.copy(phase = TimerPhase.Paused(phase, remaining))
                }
            }
            else -> { /* Can't pause from Idle, Paused, Complete */ }
        }
    }

    /**
     * Resume from paused state.
     */
    fun resume() {
        val state = _uiState.value
        val paused = state.phase as? TimerPhase.Paused ?: return

        phaseStartTime = timeProvider.uptimeMillis()

        when (val from = paused.from) {
            is TimerPhase.Work -> {
                pausedElapsedWork = paused.remainingMs
                _uiState.update { it.copy(phase = from) }
            }
            is TimerPhase.Rest -> {
                // remainingMs is the remaining rest time
                val newRestDuration = paused.remainingMs
                _uiState.update {
                    it.copy(
                        phase = TimerPhase.Rest(from.round, newRestDuration),
                        displayTimeMs = newRestDuration
                    )
                }
            }
            else -> { /* Shouldn't happen */ }
        }
    }

    /**
     * Stop the workout and reset to idle.
     */
    fun stop() {
        _uiState.update { TimerUiState() }
    }

    /**
     * Reset after workout complete.
     */
    fun reset() {
        _uiState.update { TimerUiState() }
    }

    /**
     * Check if timer is in an active state (should service be running).
     */
    fun isActive(): Boolean {
        return when (_uiState.value.phase) {
            is TimerPhase.Work, is TimerPhase.Rest, is TimerPhase.Paused -> true
            else -> false
        }
    }
}
```

**PATTERN**: Mirror existing TimerViewModel structure (lines 19-152)
**IMPORTS**: TimerPhase, TimerUiState, WorkoutConfig, TimeProvider
**GOTCHA**: BeepPlayer is nullable - injected when service starts
**VALIDATE**: File compiles

---

### Task 9: CREATE `audio/BeepPlayer.kt` - SoundPool Wrapper

**IMPLEMENT**: Create SoundPool wrapper for countdown beeps

```kotlin
package com.igygtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.igygtimer.R

/**
 * Plays countdown beep sounds using SoundPool.
 * Low-latency audio for responsive 3-2-1 countdown.
 */
class BeepPlayer(context: Context) {

    private val soundPool: SoundPool
    private var beepSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            isLoaded = (status == 0)
        }

        beepSoundId = soundPool.load(context, R.raw.beep, 1)
    }

    /**
     * Play the countdown beep sound.
     * No-op if sound hasn't loaded yet.
     */
    fun playBeep() {
        if (isLoaded && beepSoundId != 0) {
            soundPool.play(
                beepSoundId,
                1.0f,  // left volume
                1.0f,  // right volume
                1,     // priority
                0,     // loop (0 = no loop)
                1.0f   // playback rate
            )
        }
    }

    /**
     * Release SoundPool resources.
     * Call when service is destroyed.
     */
    fun release() {
        soundPool.release()
    }
}
```

**PATTERN**: AudioAttributes with USAGE_ASSISTANCE_SONIFICATION for beeps
**IMPORTS**: Context, AudioAttributes, SoundPool, R
**GOTCHA**: Must call `release()` in Service.onDestroy()
**VALIDATE**: File compiles (needs beep.ogg from Task 4)

---

### Task 10: CREATE `service/TimerService.kt` - Foreground Service

**IMPLEMENT**: Create foreground service with notification, wake lock, and tick loop

```kotlin
package com.igygtimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.igygtimer.IGYGApplication
import com.igygtimer.MainActivity
import com.igygtimer.R
import com.igygtimer.audio.BeepPlayer
import com.igygtimer.model.TimerPhase
import com.igygtimer.repository.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that runs the timer tick loop.
 * Keeps timer running when app is backgrounded or screen is off.
 */
class TimerService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
        private const val WAKELOCK_TAG = "IGYGTimer::TimerWakeLock"

        const val ACTION_START = "com.igygtimer.action.START"
        const val ACTION_STOP = "com.igygtimer.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var repository: TimerRepository
    private lateinit var beepPlayer: BeepPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        // Get repository from Application container
        repository = (application as IGYGApplication).container.timerRepository

        // Create beep player and inject into repository
        beepPlayer = BeepPlayer(this)
        repository.beepPlayer = beepPlayer

        // Create notification channel
        createNotificationChannel()

        // Acquire wake lock
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startTickLoop()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.beepPlayer = null
        beepPlayer.release()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "IGYG Timer workout notifications"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Starting workout...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IGYG Timer")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startTickLoop() {
        lifecycleScope.launch {
            while (isActive) {
                repository.tick()

                // Update notification with current state
                val state = repository.uiState.value
                val notificationText = when (val phase = state.phase) {
                    is TimerPhase.Work -> {
                        val seconds = state.displayTimeMs / 1000
                        "WORK - Round ${state.currentRound}/${state.totalRounds} - ${formatTime(seconds)}"
                    }
                    is TimerPhase.Rest -> {
                        val seconds = state.displayTimeMs / 1000
                        "REST - Round ${state.currentRound}/${state.totalRounds} - ${formatTime(seconds)}"
                    }
                    is TimerPhase.Paused -> "PAUSED - Round ${state.currentRound}/${state.totalRounds}"
                    is TimerPhase.Complete -> {
                        stopSelf()
                        "Complete!"
                    }
                    is TimerPhase.Idle -> {
                        stopSelf()
                        "Idle"
                    }
                }
                updateNotification(notificationText)

                // Check if we should stop
                if (!repository.isActive()) {
                    stopSelf()
                    break
                }

                delay(50) // 20 FPS tick rate
            }
        }
    }

    private fun formatTime(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(60 * 60 * 1000L) // 1 hour max timeout
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
```

**PATTERN**: LifecycleService for built-in lifecycleScope
**IMPORTS**: All Android service/notification classes, BeepPlayer, TimerRepository
**GOTCHA**: Must call startForeground within 10 seconds of startForegroundService
**GOTCHA**: API 34+ requires ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
**VALIDATE**: File compiles

---

### Task 11: UPDATE `viewmodel/TimerViewModel.kt` - Delegate to Repository

**IMPLEMENT**: Refactor ViewModel to observe repository instead of owning state

```kotlin
package com.igygtimer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.igygtimer.IGYGApplication
import com.igygtimer.model.TimerUiState
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.repository.TimerRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for timer screens.
 * Delegates to TimerRepository for state and logic.
 * Thin layer that provides repository access to Compose UI.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimerRepository =
        (application as IGYGApplication).container.timerRepository

    val uiState: StateFlow<TimerUiState> = repository.uiState

    fun startWorkout(config: WorkoutConfig) {
        repository.startWorkout(config)
    }

    fun onWorkDone() {
        repository.onWorkDone()
    }

    fun pause() {
        repository.pause()
    }

    fun resume() {
        repository.resume()
    }

    fun stop() {
        repository.stop()
    }

    fun reset() {
        repository.reset()
    }
}
```

**PATTERN**: AndroidViewModel to access Application
**MIRROR**: Original method signatures for API compatibility
**GOTCHA**: Changed from ViewModel to AndroidViewModel (needs Application)
**VALIDATE**: `./gradlew build` succeeds

---

### Task 12: UPDATE `navigation/NavGraph.kt` - Start/Stop Service

**IMPLEMENT**: Add service lifecycle management to navigation

```kotlin
package com.igygtimer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.igygtimer.service.TimerService
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
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = { config ->
                    viewModel.startWorkout(config)
                    TimerService.startService(context)
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
                    TimerService.stopService(context)
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
                    TimerService.stopService(context)
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}
```

**IMPLEMENT**: Add `TimerService.startService(context)` when workout starts
**IMPLEMENT**: Add `TimerService.stopService(context)` when workout stops/completes
**IMPORTS**: LocalContext, TimerService
**VALIDATE**: `./gradlew build` succeeds

---

### Task 13: UPDATE `ui/screen/HomeScreen.kt` - Load Saved Settings

**IMPLEMENT**: Load last-used ratio and rounds from SettingsRepository

```kotlin
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

    // Load saved settings on first composition
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
                val ratioValue = ratio.toFloatOrNull() ?: 1.0f
                val roundsValue = rounds.toIntOrNull() ?: 10

                // Save settings for next time
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
```

**IMPLEMENT**: Load settings via LaunchedEffect on first composition
**IMPLEMENT**: Save settings when START is tapped
**IMPORTS**: LaunchedEffect, rememberCoroutineScope, LocalContext, IGYGApplication
**VALIDATE**: `./gradlew build` succeeds

---

### Task 14: CREATE `test/repository/TimerRepositoryTest.kt` - Unit Tests

**IMPLEMENT**: Create tests for TimerRepository state transitions

```kotlin
package com.igygtimer.repository

import com.igygtimer.model.TimerPhase
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerRepositoryTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var repository: TimerRepository

    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider(currentTimeMs = 1000L)
        repository = TimerRepository(timeProvider = fakeTimeProvider)
    }

    @Test
    fun `initial state is Idle`() {
        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Idle)
    }

    @Test
    fun `startWorkout transitions to Work phase`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.5f, totalRounds = 5))

        val state = repository.uiState.value
        assertTrue("Expected Work phase but got ${state.phase}", state.phase is TimerPhase.Work)
        assertEquals(1, state.currentRound)
        assertEquals(5, state.totalRounds)
        assertEquals(1.5f, state.ratio)
    }

    @Test
    fun `onWorkDone transitions to Rest with correct duration`() {
        repository.startWorkout(WorkoutConfig(ratio = 2.0f, totalRounds = 3))

        // Simulate 10 seconds of work
        fakeTimeProvider.advanceBy(10_000)
        repository.onWorkDone()

        val state = repository.uiState.value
        assertTrue("Expected Rest phase but got ${state.phase}", state.phase is TimerPhase.Rest)

        // Rest duration should be work * ratio = 10s * 2.0 = 20s
        val rest = state.phase as TimerPhase.Rest
        assertEquals(20_000L, rest.durationMs)
    }

    @Test
    fun `pause from Work preserves elapsed time`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        // Simulate 5 seconds of work
        fakeTimeProvider.advanceBy(5_000)
        repository.tick() // Update displayTimeMs
        repository.pause()

        val state = repository.uiState.value
        assertTrue("Expected Paused phase but got ${state.phase}", state.phase is TimerPhase.Paused)

        val paused = state.phase as TimerPhase.Paused
        assertTrue(paused.from is TimerPhase.Work)
        assertEquals(5_000L, paused.remainingMs)
    }

    @Test
    fun `stop resets to Idle`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        repository.stop()

        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Idle)
    }

    @Test
    fun `tick updates displayTimeMs during Work`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(2_500)
        repository.tick()

        val state = repository.uiState.value
        assertEquals(2_500L, state.displayTimeMs)
    }

    @Test
    fun `tick counts down during Rest`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        // Complete 10 seconds of work
        fakeTimeProvider.advanceBy(10_000)
        repository.onWorkDone()

        // Advance 3 seconds into rest
        fakeTimeProvider.advanceBy(3_000)
        repository.tick()

        val state = repository.uiState.value
        // Rest was 10s, 3s elapsed, should show 7s remaining
        assertEquals(7_000L, state.displayTimeMs)
    }

    @Test
    fun `rest complete advances to next round`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        // Complete work
        fakeTimeProvider.advanceBy(5_000)
        repository.onWorkDone()

        // Complete rest (5 seconds at 1:1 ratio)
        fakeTimeProvider.advanceBy(5_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue("Expected Work phase but got ${state.phase}", state.phase is TimerPhase.Work)
        assertEquals(2, state.currentRound)
    }

    @Test
    fun `final rest complete transitions to Complete`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 1))

        // Complete work
        fakeTimeProvider.advanceBy(5_000)
        repository.onWorkDone()

        // Complete rest
        fakeTimeProvider.advanceBy(5_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue("Expected Complete phase but got ${state.phase}", state.phase is TimerPhase.Complete)
    }

    @Test
    fun `isActive returns true during workout`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        assertTrue(repository.isActive())
    }

    @Test
    fun `isActive returns false when Idle`() {
        assertTrue(!repository.isActive())
    }
}
```

**PATTERN**: Follow existing TimerViewModelTest structure
**IMPORTS**: FakeTimeProvider, TimerPhase, WorkoutConfig
**VALIDATE**: `./gradlew test --tests "com.igygtimer.repository.TimerRepositoryTest"` passes

---

### Task 15: Verify Full Build and Tests

**IMPLEMENT**: Run full build and test suite

```bash
# Clean and build
./gradlew clean build

# Run all tests
./gradlew test

# Run lint
./gradlew lint
```

**VALIDATE**: All commands succeed with no errors

---

### Task 16: Manual Testing on Device

**IMPLEMENT**: Test on physical device or emulator

1. Install app: `./gradlew installDebug`
2. Grant notification permission when prompted
3. Start workout with 1:1 ratio, 3 rounds
4. Verify:
   - [ ] Notification appears showing timer status
   - [ ] Lock screen - timer continues
   - [ ] Beeps play at 3, 2, 1 seconds of each rest phase
   - [ ] Complete workout - notification clears
   - [ ] Next launch shows saved ratio/rounds

**VALIDATE**: All manual tests pass

---

## TESTING STRATEGY

### Unit Tests

| Test Class | Coverage |
|------------|----------|
| `TimerRepositoryTest` | State transitions, tick behavior, pause/resume |
| `TimeUtilsTest` | Time formatting (existing) |
| `TimerViewModelTest` | Delegation to repository (update existing) |

### Integration Tests

Manual testing on device covers:
- Service lifecycle (start/stop)
- Notification updates
- Audio playback
- Background execution
- Settings persistence

### Edge Cases to Test

- [ ] Lock phone during work phase - timer continues
- [ ] Lock phone during rest phase - beeps still play
- [ ] Receive phone call during workout - timer continues after
- [ ] Force stop app - service terminates cleanly
- [ ] Start new workout immediately after completing one

---

## VALIDATION COMMANDS

### Level 1: Syntax & Style

```bash
./gradlew lint
```

### Level 2: Type Checking

```bash
./gradlew compileDebugKotlin
```

### Level 3: Unit Tests

```bash
./gradlew test
./gradlew test --tests "com.igygtimer.repository.TimerRepositoryTest"
```

### Level 4: Build

```bash
./gradlew build
./gradlew assembleDebug
```

### Level 5: Install and Manual Test

```bash
./gradlew installDebug
# Then manually test on device
```

---

## ACCEPTANCE CRITERIA

- [ ] Timer continues running when phone is locked (screen off)
- [ ] Timer continues running when app is backgrounded
- [ ] Beeps play at 3, 2, 1 seconds remaining during rest phase
- [ ] Persistent notification shows current timer state
- [ ] Tapping notification returns to timer screen
- [ ] Last-used ratio and rounds are saved and restored
- [ ] All unit tests pass
- [ ] App builds without warnings
- [ ] No battery drain warnings in Android Vitals (acceptable wake lock usage)

---

## COMPLETION CHECKLIST

- [ ] All tasks completed in order
- [ ] Each task validation passed immediately
- [ ] All validation commands executed successfully
- [ ] Full test suite passes (unit tests)
- [ ] No linting or type checking errors
- [ ] Manual testing confirms feature works
- [ ] Acceptance criteria all met

---

## NOTES

### Design Decisions

1. **LifecycleService over plain Service** - Provides `lifecycleScope` automatically, reducing boilerplate.

2. **AppContainer over Hilt** - Keeps dependencies simple for this small app. Can migrate to Hilt later if needed.

3. **BeepPlayer as nullable injection** - Repository doesn't own BeepPlayer lifecycle. Service creates it and injects when running.

4. **No state persistence on force-stop** - Foreground service should prevent system kills. If user force-stops, they intended to end workout.

5. **specialUse service type** - Required for API 34+ since there's no dedicated timer type. Justification provided in manifest property.

### Known Limitations

- **Manufacturer battery optimization** - Some devices (Samsung, Xiaomi) may still kill the service. Document user instructions to disable battery optimization for the app.

- **No workout recovery** - If app is force-stopped, workout progress is lost. Acceptable per design decision.

### Future Enhancements (not in scope)

- Final round distinct beep sound
- Vibration as backup notification
- Dark/light theme toggle (Phase 3)
- Workout history/logging

### Sound Asset

If you don't have a beep.ogg file, you can:
1. Generate one at https://www.soundjay.com/beep-sounds-1.html
2. Use Android ToneGenerator programmatically as fallback
3. Download a royalty-free beep sound

Recommended: 440Hz sine wave, 150ms duration, OGG format, < 50KB
