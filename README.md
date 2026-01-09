# Music Sheet Scanner

An Android app that scans sheet music using your phone's camera and plays it back through Optical Music Recognition (OMR) and audio synthesis.

## Features

### Camera & Import
- **Camera Capture**: Use your phone's camera to capture images of sheet music
- **Real-time Preview**: Live overlay showing detected staff lines and notes on camera preview
- **Gallery Import**: Load existing images from your device photos

### Music Recognition (OMR)
- **Staff Detection**: Automatic detection of staff lines using horizontal projection analysis
- **Multi-Clef Support**: Treble, bass, alto, and tenor clef recognition
- **Grand Staff**: Linked treble and bass staff detection for piano scores
- **Note Detection**: Blob detection with position-to-pitch mapping
- **Duration Recognition**: Whole, half, quarter, eighth, sixteenth, and thirty-second notes
- **Beam & Flag Detection**: Proper recognition of beamed and flagged notes
- **Rest Detection**: All standard rest durations (whole through sixteenth)
- **Chord Recognition**: Multiple simultaneous notes on the same stem
- **Time Signatures**: 4/4, 3/4, 6/8, 2/4, 2/2 detection
- **Key Signatures**: Sharp and flat detection with proper pitch mapping

### Audio Playback
- **Synthesized Audio**: Sine wave synthesis with harmonics and ADSR envelope
- **Playback Controls**: Play, pause, stop with progress tracking
- **Variable Tempo**: Adjustable BPM control
- **Transpose**: Shift all notes -12 to +12 semitones
- **Loop Mode**: Toggle infinite replay of the score
- **Metronome**: Optional click track overlay during playback

### Library & Export
- **Score History**: Save and organize your scanned scores
- **MIDI Export**: Export recognized scores as standard .mid files
- **Share**: Share MIDI files with other apps

## Screenshots

*Camera capture with real-time preview -> Processing -> Playback with controls*

## Architecture

Built with modern Android development practices:

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Declarative UI with Material 3
- **CameraX** - Camera capture and analysis
- **MVVM** - Single ViewModel with StateFlow
- **Coroutines** - Asynchronous processing

### Project Structure

```
app/src/main/java/com/musicscanner/app/
├── MainActivity.kt              # Entry point
├── MusicScannerApp.kt          # Application singleton
├── ui/
│   ├── Navigation.kt           # Compose Navigation routes
│   ├── screens/
│   │   ├── HomeScreen.kt       # Home with gallery import
│   │   ├── CameraScreen.kt     # Camera capture with preview overlay
│   │   ├── ProcessingScreen.kt # Processing status
│   │   ├── PlaybackScreen.kt   # Playback with controls
│   │   └── HistoryScreen.kt    # Score library
│   ├── viewmodel/
│   │   └── MusicScannerViewModel.kt
│   └── theme/                  # Material 3 theming
├── recognition/
│   ├── ImageProcessor.kt       # Image preprocessing pipeline
│   └── MusicRecognizer.kt      # OMR engine with multi-clef support
├── audio/
│   ├── AudioSynthesizer.kt     # Sine wave synthesis with ADSR
│   ├── MusicPlayer.kt          # Playback controller
│   └── MidiExporter.kt         # MIDI file export
└── data/
    ├── MusicModels.kt          # Data classes and enums
    └── ScoreRepository.kt      # Score persistence
```

## How It Works

### 1. Image Capture
CameraX captures high-quality images with optional real-time preview showing detected musical elements.

### 2. Image Processing
- Grayscale conversion using standard luminance formula
- Otsu's thresholding for adaptive binarization
- Horizontal projection analysis for staff line detection

### 3. Music Recognition
- Staff line detection and grouping (including grand staff)
- Clef identification (treble, bass, alto, tenor)
- Time and key signature detection
- Note head detection via blob detection with flood fill
- Beam and flag analysis for note durations
- Rest symbol recognition
- Position-to-pitch mapping based on clef type

### 4. Audio Synthesis
- MIDI note number calculation from pitch and octave
- Sine wave generation with 2nd and 3rd harmonics
- ADSR envelope (Attack: 10ms, Decay: 50ms, Sustain: 70%, Release: 100ms)
- PCM streaming via Android AudioTrack

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34

### Build Commands

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
```

## Requirements

- **Android**: API 26+ (Android 8.0 Oreo)
- **Permissions**: Camera (required), Storage (for gallery import on Android 12 and below)

## Tips for Best Results

- Use good lighting with even illumination
- Hold the camera parallel to the sheet music
- Ensure the entire staff is visible in frame
- Avoid shadows and glare on the page
- Higher resolution images yield better recognition

## Current Limitations

- Uses blob detection (no ML-based recognition yet)
- Sine wave synthesis only (no instrument sounds/SoundFont)
- No MusicXML export (MIDI only)
- No PDF import
- No score editing UI
- Falls back to demo score when recognition confidence is low

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features and progress tracking.

### Planned Features
- ML-based note detection for improved accuracy
- Instrument sounds via SoundFont/SF2
- MusicXML export for notation software
- PDF import support
- Score editing UI
- Multi-page scanning
- Auto-crop and perspective correction

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

MIT License
