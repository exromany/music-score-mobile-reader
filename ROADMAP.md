# Roadmap

Future features and improvements for Music Sheet Scanner.

## Progress Summary

| Category | Done | Total | Progress |
|----------|------|-------|----------|
| Recognition | 8 | 9 | 89% |
| Audio & Playback | 5 | 7 | 71% |
| User Experience | 3 | 8 | 38% |
| Camera & Image | 1 | 5 | 20% |
| Technical | 1 | 6 | 17% |
| **Overall** | **18** | **35** | **51%** |

## Recognition Improvements

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **ML-based note detection** | Replace blob detection with TensorFlow Lite model for higher accuracy | High | Planned |
| **Bass clef support** | Add staff position mapping for bass clef in `MusicRecognizer.kt` | Medium | Done |
| **Grand staff recognition** | Detect and link treble + bass clef staves together | High | Done |
| **Time signature detection** | Recognize 4/4, 3/4, 6/8 etc. at staff beginning | Medium | Done |
| **Key signature detection** | Identify sharps/flats after clef | Medium | Done |
| **Eighth/sixteenth notes** | Detect beamed notes and flags for faster durations | Medium | Done |
| **Rest detection** | Identify whole, half, quarter, eighth rests | Medium | Done |
| **Chord recognition** | Detect multiple simultaneous notes on same stem | High | Done |
| **Alto/Tenor clef support** | Staff position mapping for C clefs | Medium | Done |

## Audio & Playback

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Instrument sounds** | Add piano, violin, flute samples via SoundFont/SF2 | Medium | Planned |
| **MIDI export** | Export recognized score as .mid file | Low | Done |
| **MusicXML export** | Export as .musicxml for notation software | Medium | Planned |
| **Metronome overlay** | Optional click track during playback | Low | Done |
| **Loop sections** | Select and repeat specific measures | Low | Done |
| **Transpose** | Shift all notes up/down by semitones | Low | Done |
| **Variable tempo** | Support ritardando/accelerando markings | Medium | Done |

## User Experience

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Gallery import** | Load images from device photos | Low | Done |
| **PDF import** | Extract pages from PDF sheet music | Medium | Planned |
| **Score editing** | Tap notes to correct pitch/duration mistakes | Medium | Planned |
| **History/library** | Save and organize scanned scores | Low | Done |
| **Share scores** | Export as image, PDF, or audio file | Low | Partial (MIDI) |
| **Dark mode** | Theme infrastructure exists, needs toggle | Low | Partial |
| **Landscape mode** | Better view for wide scores | Low | Planned |
| **Pinch-to-zoom** | Zoom into score during playback | Low | Planned |

## Camera & Image

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Real-time preview** | Show detected notes overlay on camera preview | High | Done |
| **Multi-page scanning** | Scan multiple pages into single score | Medium | Planned |
| **Auto-crop** | Detect sheet music edges and crop automatically | Medium | Planned |
| **Perspective correction** | Fix skewed/angled captures | Medium | Planned |
| **Batch processing** | Queue multiple images for recognition | Low | Planned |

## Technical Improvements

| Feature | Description | Complexity | Status |
|---------|-------------|------------|--------|
| **Unit tests** | Add tests for `ImageProcessor`, `AudioSynthesizer` | Medium | Planned |
| **Dependency injection** | Add Hilt/Koin for better testability | Medium | Planned |
| **Offline caching** | Room database for score persistence | Low | Done (SharedPrefs) |
| **Error recovery** | Better handling of partial recognition failures | Low | Planned |
| **Performance profiling** | Optimize image processing for low-end devices | Medium | Planned |
| **Accessibility** | TalkBack support, content descriptions | Low | Planned |

## Recommended Priority Order

### Completed
1. ~~Gallery import~~ - Most requested feature for scanner apps
2. ~~Score history/library~~ - User retention and organization
3. ~~MIDI export~~ - Interoperability with music software
4. ~~Bass clef support~~ - Doubles usable sheet music
5. ~~Grand staff recognition~~ - Piano score support
6. ~~Time/key signature detection~~ - Proper musical context
7. ~~Real-time preview~~ - Better capture experience
8. ~~Playback controls~~ - Transpose, loop, metronome

### Next Up (High Priority)
9. **ML-based detection** - Significant accuracy improvement over blob detection
10. **Instrument sounds** - Better audio quality with SoundFont/SF2
11. **PDF import** - Support common digital sheet music format
12. **Score editing** - Allow users to correct recognition mistakes

### Future (Medium Priority)
13. **MusicXML export** - Professional notation software interoperability
14. **Multi-page scanning** - Handle longer pieces
15. **Auto-crop** - Improved capture convenience
16. **Perspective correction** - Handle angled captures
17. **Unit tests** - Code quality and reliability

### Backlog (Lower Priority)
18. Landscape mode
19. Pinch-to-zoom
20. Batch processing
21. Dark mode toggle
22. Dependency injection
23. Accessibility improvements
24. Performance profiling
