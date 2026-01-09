# Music Sheet Scanner

An Android app that scans sheet music using your phone's camera and plays it back.

## Features

- **Camera Capture**: Use your phone's camera to capture images of sheet music
- **Optical Music Recognition (OMR)**: Automatically detects staff lines, notes, and musical elements
- **Audio Playback**: Synthesizes and plays the recognized music
- **Playback Controls**: Play, pause, stop, and adjust tempo

## Architecture

The app is built with:
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI framework
- **CameraX** - Camera capture library
- **Coroutines** - Asynchronous programming

### Key Components

```
app/src/main/java/com/musicscanner/app/
├── MainActivity.kt              # Main entry point
├── MusicScannerApp.kt          # Application class
├── ui/
│   ├── Navigation.kt           # Navigation setup
│   ├── screens/
│   │   ├── HomeScreen.kt       # Home/landing screen
│   │   ├── CameraScreen.kt     # Camera capture screen
│   │   ├── ProcessingScreen.kt # Processing status screen
│   │   └── PlaybackScreen.kt   # Music playback screen
│   ├── viewmodel/
│   │   └── MusicScannerViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── recognition/
│   ├── ImageProcessor.kt       # Image preprocessing
│   └── MusicRecognizer.kt      # OMR engine
├── audio/
│   ├── AudioSynthesizer.kt     # Sound synthesis
│   └── MusicPlayer.kt          # Playback control
└── data/
    └── MusicModels.kt          # Data models
```

## How It Works

### 1. Image Capture
The app uses CameraX to capture high-quality images of sheet music.

### 2. Image Processing
- Convert to grayscale
- Apply Otsu's thresholding for binarization
- Detect horizontal staff lines using projection analysis

### 3. Music Recognition
- Detect note heads using blob detection
- Determine note positions relative to staff lines
- Map positions to pitches (treble clef)
- Classify note durations (filled vs hollow)

### 4. Audio Synthesis
- Convert notes to MIDI note numbers
- Generate audio samples using sine wave synthesis with harmonics
- Apply ADSR envelope for natural sound
- Stream audio through Android's AudioTrack

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34

### Build Steps
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator (API 26+)

```bash
./gradlew assembleDebug
```

## Permissions

The app requires:
- **Camera** - To capture sheet music images

## Limitations

This is a proof-of-concept implementation. Current limitations include:
- Best results with clear, well-lit sheet music
- Supports treble clef primarily
- Quarter and half notes detection
- Single staff recognition

## Future Improvements

- [ ] Machine learning-based note detection
- [x] Support for multiple staves (grand staff)
- [x] Bass clef support
- [x] Time signature detection
- [x] Key signature handling
- [ ] Articulation and dynamics
- [x] Export to MIDI file
- [x] Import from gallery

## Contributing

This project uses [Conventional Commits](https://conventionalcommits.org). Prefix your commit messages:
- `fix:` - Bug fixes (patch release)
- `feat:` - New features (minor release)
- `feat!:` - Breaking changes (major release)
- `docs:`, `chore:`, `test:` - No release

## License

MIT License
