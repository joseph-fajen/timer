# Feature: Environment Setup and Validation

The following plan configures the development environment for the IGYG Timer Android project and validates that Phase 1 implementation works correctly.

**Plan Type**: Environment/DevOps setup (not code changes)
**Execution Mode**: Mixed (some steps automated, some require human action)

## Feature Description

Set up a complete Android development environment on macOS, including Java JDK, Android SDK, Android Studio, and an emulator. Then validate that the existing Phase 1 codebase builds, tests pass, and the app runs correctly on an emulator.

## User Story

As a developer setting up this project
I want to configure my environment with Java, Android SDK, and an emulator
So that I can build, test, and run the IGYG Timer app

## Problem Statement

The Phase 1 code has been written but never validated because the development environment is not configured. Without JDK 17+, Android SDK, and an emulator, the project cannot be built or tested.

## Solution Statement

Use Homebrew to install JDK 17, install Android Studio (which bundles SDK Manager), configure environment variables, create an emulator, and run validation commands to confirm everything works.

## Feature Metadata

- **Feature Type**: Environment Setup
- **Estimated Complexity**: Low-Medium
- **Primary Systems Affected**: Local development environment (not codebase)
- **Dependencies**: Homebrew (already installed), internet connection, ~15GB disk space

---

## CONTEXT REFERENCES

### Current Environment State

| Component | Status |
|-----------|--------|
| Homebrew | Installed at `/opt/homebrew/bin/brew` |
| Java JDK | Not installed |
| JAVA_HOME | Not set |
| ANDROID_HOME | Not set |
| Android SDK | Not installed |
| Android Studio | Not installed |
| local.properties | Does not exist |

### Project Build Requirements

From `gradle/libs.versions.toml`:
- AGP 8.7.3 → requires JDK 17+
- Kotlin 2.0.21
- Compose BOM 2024.12.01

From `app/build.gradle.kts`:
- compileSdk = 35
- minSdk = 26
- targetSdk = 35

### Files That Will Be Created/Modified

- `~/.zshrc` - Add JAVA_HOME and ANDROID_HOME (append only)
- `/Users/josephfajen/git/timer/local.properties` - Create with sdk.dir path

### Relevant Documentation

- [Android Studio Download](https://developer.android.com/studio)
- [Android Studio Setup Guide](https://developer.android.com/studio/install)
- [Create and Manage Virtual Devices](https://developer.android.com/studio/run/managing-avds)

---

## IMPLEMENTATION PLAN

### Phase 1: Java Installation (Automated + Verification)

Install JDK 17 via Homebrew and configure JAVA_HOME.

### Phase 2: Android Studio Installation (Manual)

Download and install Android Studio, run Setup Wizard to install SDK.

### Phase 3: Environment Configuration (Semi-Automated)

Set environment variables and create local.properties.

### Phase 4: Emulator Setup (Manual via Android Studio)

Create an AVD (Android Virtual Device) for testing.

### Phase 5: Project Validation (Automated)

Build project, run tests, install on emulator.

### Phase 6: Functional Verification (Manual)

Verify app behavior matches Phase 1 acceptance criteria.

---

## STEP-BY-STEP TASKS

Execute tasks in order. Each task is marked:
- `[AUTO]` - Can be executed by Claude
- `[MANUAL]` - Requires human action
- `[VERIFY]` - Validation checkpoint

---

### PHASE 1: Java Installation

#### Task 1.1 [AUTO] Check if Java is already installed

```bash
java -version 2>&1
```

**Expected**: Error or version < 17
**If JDK 17+ already installed**: Skip to Task 1.4

#### Task 1.2 [AUTO] Install OpenJDK 17 via Homebrew

```bash
brew install openjdk@17
```

**Expected**: Installation completes successfully
**Time**: 1-2 minutes
**Disk space**: ~300MB

#### Task 1.3 [AUTO] Create symlink for system Java wrappers

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

**Note**: This requires sudo - Claude will prompt for permission or user runs manually.

#### Task 1.4 [VERIFY] Validate Java installation

```bash
/opt/homebrew/opt/openjdk@17/bin/java -version
```

**Expected output**: `openjdk version "17.x.x"` or higher

---

### PHASE 2: Android Studio Installation

#### Task 2.1 [MANUAL] Download Android Studio

1. Open browser to: https://developer.android.com/studio
2. Click "Download Android Studio"
3. Accept terms and download the `.dmg` file (~1.1GB)

**Time**: 5-10 minutes depending on connection

#### Task 2.2 [MANUAL] Install Android Studio

1. Open the downloaded `.dmg` file
2. Drag "Android Studio" to Applications folder
3. Eject the disk image

#### Task 2.3 [MANUAL] Run Android Studio Setup Wizard

1. Open Android Studio from Applications
2. If prompted about importing settings, select "Do not import settings"
3. The Setup Wizard will launch automatically

**In the Setup Wizard:**

4. Choose "Standard" installation type (recommended)
5. Select your preferred UI theme
6. Review components to install:
   - Android SDK
   - Android SDK Platform (API 35)
   - Android Virtual Device
7. Accept all license agreements (click each license, scroll down, click "Accept")
8. Click "Finish" and wait for downloads to complete

**Time**: 10-20 minutes (downloads ~3GB)

**IMPORTANT**: Note the SDK installation path shown in the wizard. Default is:
```
/Users/josephfajen/Library/Android/sdk
```

#### Task 2.4 [VERIFY] Confirm SDK installation

After Setup Wizard completes, verify SDK exists:

```bash
ls -la ~/Library/Android/sdk/platforms/
```

**Expected**: Directory exists with `android-35` or similar

---

### PHASE 3: Environment Configuration

#### Task 3.1 [AUTO] Check current shell configuration file

```bash
echo $SHELL
```

**Expected**: `/bin/zsh` (use `~/.zshrc`) or `/bin/bash` (use `~/.bash_profile`)

#### Task 3.2 [AUTO] Backup existing shell config

```bash
cp ~/.zshrc ~/.zshrc.backup.$(date +%Y%m%d)
```

#### Task 3.3 [AUTO] Add environment variables to shell config

Append the following to `~/.zshrc`:

```bash
cat >> ~/.zshrc << 'EOF'

# Android Development Environment (added for IGYG Timer)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
EOF
```

#### Task 3.4 [AUTO] Reload shell configuration

```bash
source ~/.zshrc
```

#### Task 3.5 [VERIFY] Validate environment variables

```bash
echo "JAVA_HOME: $JAVA_HOME"
echo "ANDROID_HOME: $ANDROID_HOME"
java -version
```

**Expected**:
- JAVA_HOME points to openjdk@17
- ANDROID_HOME points to ~/Library/Android/sdk
- java -version shows 17.x.x

#### Task 3.6 [AUTO] Create local.properties file

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > /Users/josephfajen/git/timer/local.properties
```

#### Task 3.7 [VERIFY] Confirm local.properties created

```bash
cat /Users/josephfajen/git/timer/local.properties
```

**Expected**: `sdk.dir=/Users/josephfajen/Library/Android/sdk`

---

### PHASE 4: Emulator Setup

#### Task 4.1 [MANUAL] Open AVD Manager in Android Studio

1. Open Android Studio
2. On the Welcome screen, click "More Actions" (or "Configure") dropdown
3. Select "Virtual Device Manager"

**Alternative if a project is open**: Tools → Device Manager

#### Task 4.2 [MANUAL] Create a new AVD

1. Click "Create Device" (or "+" button)
2. **Select Hardware**:
   - Category: Phone
   - Select: Pixel 7 (or Pixel 8) - good representative modern device
   - Click "Next"

3. **Select System Image**:
   - Tab: "Recommended"
   - Select: API 35 (or highest available with "Download" link if needed)
   - If download needed, click "Download" and wait
   - Click "Next"

4. **Verify Configuration**:
   - AVD Name: `Pixel_7_API_35` (or keep default)
   - Leave other settings as default
   - Click "Finish"

**Time**: 5-10 minutes if system image download needed (~1.5GB)

#### Task 4.3 [MANUAL] Start the emulator

1. In Device Manager, click the "Play" button (▶) next to your AVD
2. Wait for emulator to boot (first boot takes 1-2 minutes)

#### Task 4.4 [VERIFY] Confirm emulator is running

```bash
adb devices
```

**Expected output**:
```
List of devices attached
emulator-5554	device
```

**Note**: If `adb` is not found, you may need to open a new terminal after setting environment variables, or run:
```bash
source ~/.zshrc && adb devices
```

---

### PHASE 5: Project Validation

#### Task 5.1 [AUTO] Navigate to project directory

```bash
cd /Users/josephfajen/git/timer
```

#### Task 5.2 [AUTO] Clean any previous build artifacts

```bash
./gradlew clean
```

**Expected**: BUILD SUCCESSFUL
**Time**: First run downloads Gradle wrapper (~30 seconds)

#### Task 5.3 [AUTO] Build the project

```bash
./gradlew build
```

**Expected**: BUILD SUCCESSFUL
**Time**: First build 2-5 minutes (downloads dependencies)

**If build fails**, check:
1. JAVA_HOME is set correctly
2. local.properties exists with correct sdk.dir
3. Android SDK platform 35 is installed

#### Task 5.4 [VERIFY] Confirm build artifacts exist

```bash
ls -la app/build/outputs/apk/debug/
```

**Expected**: `app-debug.apk` exists

#### Task 5.5 [AUTO] Run unit tests

```bash
./gradlew test
```

**Expected**: BUILD SUCCESSFUL, all tests pass

#### Task 5.6 [VERIFY] Check test results

```bash
cat app/build/reports/tests/testDebugUnitTest/index.html 2>/dev/null | grep -o '[0-9]* tests' | head -1
```

**Expected**: Shows number of tests (should be > 0)

**Alternative**: Open `app/build/reports/tests/testDebugUnitTest/index.html` in browser

#### Task 5.7 [AUTO] Run lint checks

```bash
./gradlew lint
```

**Expected**: BUILD SUCCESSFUL (warnings OK, errors should be 0)

#### Task 5.8 [AUTO] Install app on emulator

**Prerequisite**: Emulator must be running (Task 4.3)

```bash
./gradlew installDebug
```

**Expected**: BUILD SUCCESSFUL, app installed

#### Task 5.9 [AUTO] Launch app on emulator

```bash
adb shell am start -n com.igygtimer/.MainActivity
```

**Expected**: App launches on emulator

---

### PHASE 6: Functional Verification

#### Task 6.1 [MANUAL] Verify Home Screen

In the emulator, verify:
- [ ] App displays "IGYG Timer" title
- [ ] Ratio presets shown (0.5, 1, 1.5, 2)
- [ ] Custom ratio input field visible
- [ ] Round count with +/- buttons visible
- [ ] START button visible

#### Task 6.2 [MANUAL] Test basic workout flow

1. Set ratio to 1:1 (tap the "1" preset)
2. Set rounds to 3
3. Tap START
4. **Verify WORK phase**:
   - [ ] Screen turns green
   - [ ] Shows "WORK" label
   - [ ] Shows "Round 1 of 3"
   - [ ] Timer counts UP from 0:00
   - [ ] "Tap anywhere when done" hint visible

5. Wait ~5 seconds, then tap screen
6. **Verify REST phase**:
   - [ ] Screen turns blue
   - [ ] Shows "REST" label
   - [ ] Timer shows ~5 seconds (same as work time)
   - [ ] Timer counts DOWN

7. Wait for rest to complete
8. **Verify round 2 starts automatically**:
   - [ ] Screen turns green again
   - [ ] Shows "Round 2 of 3"

#### Task 6.3 [MANUAL] Test pause/resume

1. During WORK or REST phase, tap PAUSE
2. **Verify PAUSED state**:
   - [ ] Screen turns orange
   - [ ] Shows "PAUSED"
   - [ ] Timer stopped
   - [ ] RESUME button visible

3. Tap RESUME
4. **Verify timer continues**:
   - [ ] Returns to previous phase color
   - [ ] Timer continues from where it stopped

#### Task 6.4 [MANUAL] Test stop confirmation

1. Tap STOP button
2. **Verify confirmation dialog**:
   - [ ] Dialog appears with "End Workout?"
   - [ ] Shows current round progress
   - [ ] CANCEL and END buttons visible

3. Tap CANCEL
4. **Verify**: Workout continues

5. Tap STOP again, then tap END
6. **Verify**: Returns to Home screen

#### Task 6.5 [MANUAL] Test complete workflow

1. Start new workout: 1:1 ratio, 2 rounds
2. Complete round 1: tap during work, wait for rest
3. Complete round 2: tap during work, wait for rest to end
4. **Verify COMPLETE screen**:
   - [ ] Shows "COMPLETE!"
   - [ ] Shows total rounds completed
   - [ ] Shows total time
   - [ ] DONE button visible

5. Tap DONE
6. **Verify**: Returns to Home screen

#### Task 6.6 [MANUAL] Verify screen stays on

1. Start a workout
2. Do not interact with emulator for 30+ seconds
3. **Verify**: Screen does not turn off during workout

---

## VALIDATION COMMANDS

Execute all commands to ensure environment is fully configured.

### Level 1: Environment

```bash
# Java version (must be 17+)
java -version

# JAVA_HOME set
echo $JAVA_HOME

# ANDROID_HOME set
echo $ANDROID_HOME

# local.properties exists
cat /Users/josephfajen/git/timer/local.properties
```

### Level 2: Build

```bash
cd /Users/josephfajen/git/timer
./gradlew build
```

### Level 3: Tests

```bash
./gradlew test
```

### Level 4: Lint

```bash
./gradlew lint
```

### Level 5: Device

```bash
# Emulator connected
adb devices

# App installed and launches
./gradlew installDebug
adb shell am start -n com.igygtimer/.MainActivity
```

---

## ACCEPTANCE CRITERIA

### Environment Setup
- [x] Java JDK 17+ installed and accessible
- [x] JAVA_HOME environment variable set
- [x] Android SDK installed (API 35)
- [x] ANDROID_HOME environment variable set
- [x] local.properties created with correct sdk.dir
- [x] Emulator created and bootable
- [x] adb can see connected emulator

### Build Validation
- [ ] `./gradlew build` succeeds with no errors
- [ ] `./gradlew test` succeeds with all tests passing
- [ ] `./gradlew lint` succeeds with no errors
- [ ] `./gradlew installDebug` succeeds

### Functional Validation (Phase 1 Acceptance Criteria)
- [ ] App launches on emulator
- [ ] Home screen: ratio selection + round count works
- [ ] Timer starts in WORK phase, counts UP
- [ ] Tapping during WORK → REST with correct duration (work × ratio)
- [ ] REST counts DOWN and auto-transitions to next WORK
- [ ] Final REST → COMPLETE screen
- [ ] Pause/Resume works from WORK and REST phases
- [ ] Stop shows confirmation dialog, returns to Home
- [ ] Screen stays on during timer

---

## COMPLETION CHECKLIST

- [ ] Phase 1: Java installed and verified
- [ ] Phase 2: Android Studio installed, SDK configured
- [ ] Phase 3: Environment variables set, local.properties created
- [ ] Phase 4: Emulator created and running
- [ ] Phase 5: Build, tests, lint all pass
- [ ] Phase 6: All functional tests verified manually

---

## TROUBLESHOOTING

### Gradle sync fails with SDK not found

**Cause**: local.properties missing or incorrect path
**Fix**:
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > /Users/josephfajen/git/timer/local.properties
```

### java: command not found after installation

**Cause**: Shell not reloaded
**Fix**:
```bash
source ~/.zshrc
```

### adb: command not found

**Cause**: ANDROID_HOME not in PATH
**Fix**:
```bash
source ~/.zshrc
# Or use full path:
~/Library/Android/sdk/platform-tools/adb devices
```

### Emulator won't start - "HAXM not installed"

**Cause**: Hardware acceleration not available (rare on Apple Silicon)
**Fix**: Apple Silicon Macs use native ARM emulation, not HAXM. Ensure you downloaded ARM64 system image.

### Build fails with "Unsupported class file major version"

**Cause**: Wrong Java version
**Fix**: Verify JAVA_HOME points to JDK 17:
```bash
echo $JAVA_HOME
# Should show: /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### Emulator runs but app crashes on launch

**Cause**: Various - check logcat
**Debug**:
```bash
adb logcat -s "AndroidRuntime:E" | head -50
```

---

## NOTES

### Time Estimates

| Phase | Estimated Time |
|-------|----------------|
| Phase 1: Java | 5 minutes |
| Phase 2: Android Studio | 15-25 minutes (download dependent) |
| Phase 3: Environment | 5 minutes |
| Phase 4: Emulator | 10 minutes |
| Phase 5: Build/Test | 10 minutes |
| Phase 6: Functional test | 10 minutes |
| **Total** | **55-65 minutes** |

### Disk Space Required

| Component | Size |
|-----------|------|
| OpenJDK 17 | ~300MB |
| Android Studio | ~1.5GB |
| Android SDK (API 35) | ~2GB |
| Emulator system image | ~1.5GB |
| Gradle cache | ~500MB |
| **Total** | **~6GB** |

### What Happens Next

After environment setup and validation:
1. Mark Phase 1 acceptance criteria as verified
2. Can proceed to Phase 2 implementation (background service, audio)
3. Consider physical device testing before any release
