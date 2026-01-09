# Roadmap

Future features and improvements for Music Sheet Scanner.

## Progress Summary

| Category | Done | Total | Progress |
|----------|------|-------|----------|
| Recognition | 8 | 9 | 89% |
| Audio & Playback | 5 | 7 | 71% |
| User Experience | 2 | 8 | 25% |
| Camera & Image | 0 | 5 | 0% |
| Technical | 1 | 6 | 17% |
| Quick Wins | 6 | 6 | 100% |
| **Overall** | **22** | **41** | **54%** |

## Recognition Improvements

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **ML-based note detection** | Replace blob detection with TensorFlow Lite model for higher accuracy | High | Planned |
| **Bass clef support** | Add staff position mapping for bass clef in `MusicRecognizer.kt` | Medium | ✅ Done |
| **Grand staff recognition** | Detect and link treble + bass clef staves together | High | ✅ Done |
| **Time signature detection** | Recognize 4/4, 3/4, 6/8 etc. at staff beginning | Medium | ✅ Done |
| **Key signature detection** | Identify sharps/flats after clef | Medium | ✅ Done |
| **Eighth/sixteenth notes** | Detect beamed notes and flags for faster durations | Medium | ✅ Done |
| **Rest detection** | Identify whole, half, quarter, eighth rests | Medium | ✅ Done |
| **Chord recognition** | Detect multiple simultaneous notes on same stem | High | ✅ Done |
| **Alto/Tenor clef support** | Staff position mapping for C clefs | Medium | ✅ Done |

## Audio & Playback

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Instrument sounds** | Add piano, violin, flute samples via SoundFont/SF2 | Medium | Planned |
| **MIDI export** | Export recognized score as .mid file | Low | ✅ Done |
| **MusicXML export** | Export as .musicxml for notation software | Medium | Planned |
| **Metronome overlay** | Optional click track during playback | Low | ✅ Done |
| **Loop sections** | Select and repeat specific measures | Low | ✅ Done |
| **Transpose** | Shift all notes up/down by semitones | Low | ✅ Done |
| **Variable tempo** | Support ritardando/accelerando markings | Medium | ✅ Done |

## User Experience

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Gallery import** | Load images from device photos | Low | ✅ Done |
| **PDF import** | Extract pages from PDF sheet music | Medium | Planned |
| **Score editing** | Tap notes to correct pitch/duration mistakes | Medium | Planned |
| **History/library** | Save and organize scanned scores | Low | ✅ Done |
| **Share scores** | Export as image, PDF, or audio file | Low | Partial (MIDI) |
| **Dark mode** | Already have theme infrastructure, just needs toggle | Low | Partial |
| **Landscape mode** | Better view for wide scores | Low | Planned |
| **Pinch-to-zoom** | Zoom into score during playback | Low | Planned |

## Camera & Image

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Real-time preview** | Show detected notes overlay on camera preview | High | Planned |
| **Multi-page scanning** | Scan multiple pages into single score | Medium | Planned |
| **Auto-crop** | Detect sheet music edges and crop automatically | Medium | Planned |
| **Perspective correction** | Fix skewed/angled captures | Medium | Planned |
| **Batch processing** | Queue multiple images for recognition | Low | Planned |

## Technical Improvements

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Unit tests** | Add tests for `ImageProcessor`, `AudioSynthesizer` | Medium | Planned |
| **Dependency injection** | Add Hilt/Koin for better testability | Medium | Planned |
| **Offline caching** | Room database for score persistence | Low | ✅ Done (SharedPrefs) |
| **Error recovery** | Better handling of partial recognition failures | Low | Planned |
| **Performance profiling** | Optimize image processing for low-end devices | Medium | Planned |
| **Accessibility** | TalkBack support, content descriptions | Low | Planned |

## Quick Wins

Low effort, high value features:

| Feature | Why It's a Quick Win | Status |
|---------|----------------------|--------|
| **Gallery import** | Just add image picker intent | ✅ Done |
| **MIDI export** | Standard format, libraries available | ✅ Done |
| **Transpose control** | Simple pitch offset in `MusicPlayer` | ✅ Done |
| **Loop playback** | Add start/end measure selection | ✅ Done |
| **Score history** | SharedPreferences or Room for saving | ✅ Done |
| **Metronome** | Simple click synthesis alongside notes | ✅ Done |

## Recommended Priority Order

### Completed
1. ~~**Gallery import** - Most requested feature for any scanner app~~ ✅
2. ~~**Score history/library** - User retention~~ ✅
3. ~~**MIDI export** - Interoperability with other music software~~ ✅
4. ~~**Bass clef support** - Doubles the amount of usable sheet music~~ ✅

### Next Up
5. **ML-based detection** - Accuracy improvement
6. **Instrument sounds** - Audio quality enhancement
7. **PDF import** - Support more input formats
8. **MusicXML export** - Better interoperability with notation software
9. **Score editing** - Allow users to correct recognition mistakes
10. **Real-time preview** - Show detection overlay on camera
