# Roadmap

Future features and improvements for Music Sheet Scanner.

## Recognition Improvements

| Feature | Description | Complexity |
|---------|-------------|------------|
| **ML-based note detection** | Replace blob detection with TensorFlow Lite model for higher accuracy | High |
| **Bass clef support** | Add staff position mapping for bass clef in `MusicRecognizer.kt` | Medium |
| **Grand staff recognition** | Detect and link treble + bass clef staves together | High |
| **Time signature detection** | Recognize 4/4, 3/4, 6/8 etc. at staff beginning | Medium |
| **Key signature detection** | Identify sharps/flats after clef | Medium |
| **Eighth/sixteenth notes** | Detect beamed notes and flags for faster durations | Medium |
| **Rest detection** | Identify whole, half, quarter, eighth rests | Medium |
| **Chord recognition** | Detect multiple simultaneous notes on same stem | High |

## Audio & Playback

| Feature | Description | Complexity |
|---------|-------------|------------|
| **Instrument sounds** | Add piano, violin, flute samples via SoundFont/SF2 | Medium |
| **MIDI export** | Export recognized score as .mid file | Low |
| **MusicXML export** | Export as .musicxml for notation software | Medium |
| **Metronome overlay** | Optional click track during playback | Low |
| **Loop sections** | Select and repeat specific measures | Low |
| **Transpose** | Shift all notes up/down by semitones | Low |
| **Variable tempo** | Support ritardando/accelerando markings | Medium |

## User Experience

| Feature | Description | Complexity |
|---------|-------------|------------|
| **Gallery import** | Load images from device photos | Low |
| **PDF import** | Extract pages from PDF sheet music | Medium |
| **Score editing** | Tap notes to correct pitch/duration mistakes | Medium |
| **History/library** | Save and organize scanned scores | Low |
| **Share scores** | Export as image, PDF, or audio file | Low |
| **Dark mode** | Already have theme infrastructure, just needs toggle | Low |
| **Landscape mode** | Better view for wide scores | Low |
| **Pinch-to-zoom** | Zoom into score during playback | Low |

## Camera & Image

| Feature | Description | Complexity |
|---------|-------------|------------|
| **Real-time preview** | Show detected notes overlay on camera preview | High |
| **Multi-page scanning** | Scan multiple pages into single score | Medium |
| **Auto-crop** | Detect sheet music edges and crop automatically | Medium |
| **Perspective correction** | Fix skewed/angled captures | Medium |
| **Batch processing** | Queue multiple images for recognition | Low |

## Technical Improvements

| Feature | Description | Complexity |
|---------|-------------|------------|
| **Unit tests** | Add tests for `ImageProcessor`, `AudioSynthesizer` | Medium |
| **Dependency injection** | Add Hilt/Koin for better testability | Medium |
| **Offline caching** | Room database for score persistence | Low |
| **Error recovery** | Better handling of partial recognition failures | Low |
| **Performance profiling** | Optimize image processing for low-end devices | Medium |
| **Accessibility** | TalkBack support, content descriptions | Low |

## Quick Wins

Low effort, high value features:

| Feature | Why It's a Quick Win |
|---------|----------------------|
| **Gallery import** | Just add image picker intent |
| **MIDI export** | Standard format, libraries available |
| **Transpose control** | Simple pitch offset in `MusicPlayer` |
| **Loop playback** | Add start/end measure selection |
| **Score history** | SharedPreferences or Room for saving |
| **Metronome** | Simple click synthesis alongside notes |

## Recommended Priority Order

1. **Gallery import** - Most requested feature for any scanner app
2. **Score history/library** - User retention
3. **MIDI export** - Interoperability with other music software
4. **Bass clef support** - Doubles the amount of usable sheet music
5. **ML-based detection** - Accuracy improvement
6. **Instrument sounds** - Audio quality enhancement
