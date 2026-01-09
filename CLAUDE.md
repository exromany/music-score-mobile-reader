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
│   ├── screens/                # UI screens (Home, Camera, Processing, Playback, History)
│   │   ├── HomeScreen.kt       # Home with gallery import
│   │   ├── CameraScreen.kt     # Camera capture
│   │   ├── ProcessingScreen.kt # Image processing status
│   │   ├── PlaybackScreen.kt   # Playback with controls (loop, transpose, metronome)
│   │   └── HistoryScreen.kt    # Score library/history
│   ├── viewmodel/              # MusicScannerViewModel (single ViewModel)
│   └── theme/                  # Material 3 theming (Color, Type, Theme)
├── recognition/
│   ├── MusicRecognizer.kt      # OMR orchestrator (multi-clef support)
│   └── ImageProcessor.kt       # Image preprocessing pipeline (enhanced detection)
├── audio/
│   ├── AudioSynthesizer.kt     # Sine wave synthesis with ADSR
│   ├── MusicPlayer.kt          # Playback controller (loop, transpose, metronome)
│   └── MidiExporter.kt         # MIDI file export functionality
└── data/
    ├── MusicModels.kt          # Data classes and enums
    └── ScoreRepository.kt      # Score history persistence
```

## Architecture Patterns

- **MVVM**: Single `MusicScannerViewModel` manages all app state via `StateFlow`
- **State Management**: Sealed classes for `ProcessingState` and `PlaybackState`
- **Async Operations**: Coroutines with `Dispatchers.Default/IO` for heavy processing
- **Navigation**: Jetpack Compose Navigation with sealed class routes

## Key Data Models (in MusicModels.kt)

- `MusicNote`: Pitch, octave, duration, position, accidentals, isRest, isDotted, ties, MIDI conversion
- `MusicScore`: Collection of staves with metadata
- `Staff`: Clef type, measures, time/key signatures
- `Measure`: Notes grouped with time/key signatures
- `TimeSignature`: Common time (4/4), waltz (3/4), cut time (2/2), and custom
- `KeySignature`: Tracks sharps/flats with affected pitch mapping
- `PlaybackState`: Includes transpose, loop, and metronome state
- Enums:
  - `Pitch`: A through G
  - `NoteDuration`: WHOLE, HALF, QUARTER, EIGHTH, SIXTEENTH, THIRTY_SECOND
  - `Accidental`: NONE, SHARP, FLAT, NATURAL, DOUBLE_SHARP, DOUBLE_FLAT
  - `Clef`: TREBLE, BASS, ALTO, TENOR

## Code Conventions

- Kotlin naming: camelCase for variables/functions, PascalCase for classes
- Composable functions prefixed with screen/component purpose (e.g., `HomeScreen`, `PlaybackControls`)
- All heavy operations must use coroutines (never block main thread)
- State updates flow through ViewModel StateFlow emissions

## Image Processing Pipeline

1. Grayscale conversion (standard luminance formula)
2. Binarization via Otsu's thresholding
3. Staff line detection via horizontal projection
4. Grand staff detection (linked treble + bass staves)
5. Time signature detection (4/4, 3/4, 6/8, 2/4, 2/2)
6. Key signature detection (sharps and flats)
7. Note head detection via blob detection with flood fill
8. Beam and flag detection for eighth/sixteenth notes
9. Rest detection (whole, half, quarter, eighth, sixteenth)
10. Chord detection (multiple notes on same stem)
11. Position-to-pitch mapping (treble, bass, alto, tenor clefs)

## Audio Synthesis & Playback

- Sine wave generation with harmonics (fundamental + 2nd/3rd)
- ADSR envelope: Attack 10ms, Decay 50ms, Sustain 70%, Release 100ms
- MIDI frequency formula: A4 = 440Hz
- Output via Android AudioTrack (PCM)

### Playback Features
- **Transpose**: Shift all notes -12 to +12 semitones
- **Loop**: Toggle infinite replay of score
- **Metronome**: Optional click track overlay during playback
- **Variable tempo**: Adjustable BPM control

### Export
- **MIDI export**: Standard MIDI file format (.mid) with share intent support

## Current Limitations

- No ML-based note detection (uses blob detection)
- No MusicXML export (MIDI only)
- No instrument sounds/SoundFont (sine wave synthesis only)
- No PDF import
- No score editing UI
- Falls back to demo score (Twinkle Twinkle) when recognition fails

## Recently Implemented Features

- Multi-clef support (treble, bass, alto, tenor)
- Grand staff recognition
- Time and key signature detection
- Rest detection (all durations)
- Eighth/sixteenth/thirty-second notes with beam/flag detection
- Chord recognition
- Gallery import from device photos
- Score history/library with persistence
- MIDI export with sharing
- Transpose, loop, and metronome playback controls
- Real-time preview overlay on camera (staff lines and note detection)

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
