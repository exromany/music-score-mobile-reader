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

## Release Process

Uses [Conventional Commits](https://conventionalcommits.org) + [release-please](https://github.com/googleapis/release-please):

### Commit Format
- `fix: message` → patch (1.0.0 → 1.0.1)
- `feat: message` → minor (1.0.0 → 1.1.0)
- `feat!: message` → major (1.0.0 → 2.0.0)

### How It Works
1. Push conventional commits to main
2. release-please creates/updates Release PR
3. Merge Release PR → GitHub release + APK built

### Examples
```
fix: correct note detection threshold
feat: add PDF import support
feat!: redesign playback API
docs: update README
chore: update dependencies
```

## Project Structure

```
app/src/main/java/com/musicscanner/app/
├── MainActivity.kt              # Entry point
├── MusicScannerApp.kt          # Application singleton
├── ui/
│   ├── Navigation.kt           # Compose Navigation routes
│   ├── screens/                # UI screens (Home, Camera, Processing, Playback, History, MultiPage)
│   │   ├── HomeScreen.kt       # Home with gallery import and multi-page option
│   │   ├── CameraScreen.kt     # Camera capture with real-time preview
│   │   ├── ProcessingScreen.kt # Image processing status
│   │   ├── PlaybackScreen.kt   # Playback with controls (loop, transpose, metronome)
│   │   ├── HistoryScreen.kt    # Score library/history
│   │   └── MultiPageScanScreen.kt # Multi-page scanning with batch support
│   ├── viewmodel/              # MusicScannerViewModel (single ViewModel)
│   └── theme/                  # Material 3 theming (Color, Type, Theme)
├── recognition/
│   ├── MusicRecognizer.kt      # OMR orchestrator (multi-clef, multi-page support)
│   ├── ImageProcessor.kt       # Image preprocessing pipeline (enhanced detection)
│   └── ImageEnhancer.kt        # Auto-crop, perspective correction, contrast enhancement
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

### Image Enhancement (ImageEnhancer)
1. **Auto-crop**: Detect sheet music boundaries via projection analysis
2. **Perspective detection**: Analyze horizontal lines for skew angle
3. **Skew correction**: Rotate image to straighten staff lines
4. **Contrast enhancement**: Histogram-based adaptive stretching

### Music Recognition (ImageProcessor)
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

### Multi-Page Processing (MusicRecognizer)
- Process multiple images sequentially
- Merge scores with measure offset tracking
- Combine staves with matching clefs

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
- **Auto-crop**: Automatic detection and cropping of sheet music boundaries
- **Perspective correction**: Skew detection and automatic straightening
- **Multi-page scanning**: Scan multiple pages and merge into single score
- **Batch processing**: Queue multiple images for sequential recognition

## Dependencies

Key libraries (see app/build.gradle.kts for versions):
- AndroidX Compose BOM 2023.10.01
- CameraX 1.3.0
- ML Kit image-labeling 17.0.7
- Navigation Compose 2.7.5
- Accompanist Permissions 0.32.0
- Coroutines 1.7.3
- Coil Compose 2.5.0 (image loading for thumbnails)

## Permissions

- `CAMERA` (required): Sheet music capture
- Storage permissions for Android 12 and below

## Testing

### Running Tests
```bash
# Run all unit tests
./gradlew test

# Run tests for specific module
./gradlew :app:test

# Run with coverage report
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Test Files Location
- Unit tests: `app/src/test/java/com/musicscanner/app/`
- Instrumented tests: `app/src/androidTest/java/com/musicscanner/app/`

### Key Test Areas
- `ImageProcessorTest`: Grayscale conversion, Otsu thresholding, staff detection
- `MusicRecognizerTest`: Note detection, pitch mapping, clef handling
- `AudioSynthesizerTest`: Frequency calculations, ADSR envelope

## Development Workflow

1. **Before making changes**: Run `./gradlew test` to ensure tests pass
2. **After changes**: Run tests again and verify on emulator/device
3. **For UI changes**: Test on multiple screen sizes (phone and tablet)
4. **For recognition changes**: Test with sample sheet music images in `app/src/test/resources/`

## Common Issues

### Build Issues
- If Gradle sync fails, try `./gradlew clean` then sync again
- Ensure JDK 17 is configured in Android Studio

### Runtime Issues
- Camera permission must be granted for capture features
- Gallery import requires storage permissions on Android 12 and below
- Real-time preview may lag on low-end devices due to image processing

### Recognition Quality
- Best results with clear, well-lit images
- Handwritten music is not supported
- Complex orchestral scores may not parse correctly
