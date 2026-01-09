# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Music Sheet Scanner is an Android app that captures sheet music via camera and plays it back through Optical Music Recognition (OMR) and audio synthesis.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **Camera**: CameraX
- **Build System**: Gradle 8.2 with Kotlin DSL
- **Min SDK**: 26 (Android 8.0), Target SDK: 34

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Project Structure

```
app/src/main/java/com/musicscanner/app/
├── MainActivity.kt              # Entry point
├── MusicScannerApp.kt          # Application singleton
├── ui/
│   ├── Navigation.kt           # Compose Navigation routes
│   ├── screens/                # UI screens (Home, Camera, Processing, Playback)
│   ├── viewmodel/              # MusicScannerViewModel (single ViewModel)
│   └── theme/                  # Material 3 theming (Color, Type, Theme)
├── recognition/
│   ├── MusicRecognizer.kt      # OMR orchestrator
│   └── ImageProcessor.kt       # Image preprocessing pipeline
├── audio/
│   ├── AudioSynthesizer.kt     # Sine wave synthesis with ADSR
│   └── MusicPlayer.kt          # Playback controller
└── data/
    └── MusicModels.kt          # Data classes and enums
```

## Architecture Patterns

- **MVVM**: Single `MusicScannerViewModel` manages all app state via `StateFlow`
- **State Management**: Sealed classes for `ProcessingState` and `PlaybackState`
- **Async Operations**: Coroutines with `Dispatchers.Default/IO` for heavy processing
- **Navigation**: Jetpack Compose Navigation with sealed class routes

## Key Data Models (in MusicModels.kt)

- `MusicNote`: Pitch, octave, duration, position, accidentals
- `MusicScore`: Collection of staves with metadata
- `Staff`: Clef type, measures, signatures
- `Measure`: Notes grouped with time/key signatures
- Enums: `Pitch`, `NoteDuration`, `Accidental`, `Clef`

## Code Conventions

- Kotlin naming: camelCase for variables/functions, PascalCase for classes
- Composable functions prefixed with screen/component purpose (e.g., `HomeScreen`, `PlaybackControls`)
- All heavy operations must use coroutines (never block main thread)
- State updates flow through ViewModel StateFlow emissions

## Image Processing Pipeline

1. Grayscale conversion (standard luminance formula)
2. Binarization via Otsu's thresholding
3. Staff line detection via horizontal projection
4. Note head detection via blob detection with flood fill
5. Position-to-pitch mapping (treble clef)

## Audio Synthesis

- Sine wave generation with harmonics (fundamental + 2nd/3rd)
- ADSR envelope: Attack 10ms, Decay 50ms, Sustain 70%, Release 100ms
- MIDI frequency formula: A4 = 440Hz
- Output via Android AudioTrack (PCM)

## Current Limitations

- Treble clef only (staff position mapping hardcoded)
- Quarter and half notes only
- Single staff per image
- Falls back to demo score (Twinkle Twinkle) when recognition fails

## Dependencies

Key libraries (see app/build.gradle.kts for versions):
- AndroidX Compose BOM 2023.10.01
- CameraX 1.3.0
- ML Kit image-labeling 17.0.7
- Navigation Compose 2.7.5
- Accompanist Permissions 0.32.0
- Coroutines 1.7.3

## Permissions

- `CAMERA` (required): Sheet music capture
- Storage permissions for Android 12 and below
