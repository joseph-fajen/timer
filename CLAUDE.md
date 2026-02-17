# IGYG Timer - Project Rules

## Project Overview

Native Android workout timer for "I Go You Go" (IGYG) training. Calculates rest periods dynamically based on work time × configurable ratio.

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM with StateFlow
- **Navigation**: Compose Navigation
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Build**: Gradle Kotlin DSL with version catalogs

## Project Structure

```
app/src/main/kotlin/com/igygtimer/
├── MainActivity.kt           # Single activity entry point
├── model/                    # Data classes, sealed classes for state
├── viewmodel/                # ViewModels with StateFlow
├── ui/
│   ├── screen/               # Full-screen composables
│   ├── component/            # Reusable UI components
│   └── theme/                # Colors, typography, theme
├── navigation/               # NavGraph and routes
└── util/                     # Pure utility functions
```

## Code Conventions

### State Management
- Use `StateFlow` with `MutableStateFlow` for UI state
- Use sealed classes for type-safe state variants (see `TimerPhase`)
- Expose state as `StateFlow<T>` (immutable), update via `_state.update { }`

### Compose
- Screens are top-level composables in `ui/screen/`
- Reusable components go in `ui/component/`
- Pass callbacks down, hoist state up
- Use `remember` for local UI state, ViewModel for business state

### Timing
- Use `SystemClock.uptimeMillis()` for drift-free timing (not `System.currentTimeMillis()`)
- Timer tick rate: 50ms delay (20 FPS updates)

### Naming
- Screens: `XxxScreen.kt` with `@Composable fun XxxScreen(...)`
- Components: `XxxComponent.kt` or descriptive name like `TimeDisplay.kt`
- ViewModels: `XxxViewModel.kt`
- State classes: `XxxUiState` data class with `XxxPhase` sealed class

## Build Commands

```bash
# Build
./gradlew build

# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.igygtimer.util.TimeUtilsTest"
./gradlew test --tests "com.igygtimer.viewmodel.TimerViewModelTest"

# Lint
./gradlew lint

# Install debug APK
./gradlew installDebug

# Generate release APK
./gradlew assembleRelease
```

## Testing

- **Unit tests**: JUnit 4 + kotlinx-coroutines-test + Turbine
- **Test location**: `app/src/test/kotlin/com/igygtimer/`
- **Pattern**: Test pure functions in `util/`, test ViewModel state transitions with Turbine
- Place tests in matching package structure (e.g., `viewmodel/TimerViewModelTest.kt`)

## Dependencies

Managed via version catalog at `gradle/libs.versions.toml`. Add new dependencies there first, then reference in `build.gradle.kts` as `libs.xxx`.

## Implementation Plans

Feature plans live in `.agents/plans/`. Reference these for context on design decisions and acceptance criteria.

## Current Phase

Phase 1 (core timer) is implemented. Phase 2 will add:
- Background service execution
- Audio beeps (3, 2, 1 countdown)
- Settings persistence
