package com.musicscanner.app.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.musicscanner.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main class for Optical Music Recognition (OMR)
 * Coordinates the image processing pipeline to recognize music notation
 */
class MusicRecognizer(private val context: Context) {

    private val imageProcessor = ImageProcessor()

    /**
     * Process an image and extract music notation
     */
    suspend fun recognize(
        imagePath: String,
        onProgressUpdate: (ProcessingState) -> Unit
    ): MusicScore = withContext(Dispatchers.Default) {

        try {
            // Step 1: Load image
            onProgressUpdate(ProcessingState.LoadingImage)
            val bitmap = loadImage(imagePath)
                ?: throw RecognitionException("Failed to load image")

            // Step 2: Preprocess image
            onProgressUpdate(ProcessingState.PreprocessingImage)
            val grayscale = imageProcessor.toGrayscale(bitmap)
            val binary = imageProcessor.binarize(grayscale, bitmap.width, bitmap.height)

            // Step 3: Detect staff lines
            onProgressUpdate(ProcessingState.DetectingStaffLines)
            val staffLines = imageProcessor.detectStaffLines(binary, bitmap.width, bitmap.height)

            if (staffLines.isEmpty()) {
                // If no staff lines detected, generate a simple demo score
                return@withContext generateDemoScore()
            }

            // Step 4: Recognize musical elements
            onProgressUpdate(ProcessingState.RecognizingNotes)
            val noteHeads = imageProcessor.detectNoteHeads(
                binary, bitmap.width, bitmap.height, staffLines
            )

            // Step 5: Build music score
            onProgressUpdate(ProcessingState.GeneratingMidi)
            val score = buildMusicScore(staffLines, noteHeads)

            onProgressUpdate(ProcessingState.Complete(score))
            score

        } catch (e: Exception) {
            onProgressUpdate(ProcessingState.Error(e.message ?: "Unknown error"))
            throw e
        }
    }

    /**
     * Load image from file path or URI
     */
    private fun loadImage(path: String): Bitmap? {
        return try {
            if (path.startsWith("content://") || path.startsWith("file://")) {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build MusicScore from detected elements
     */
    private fun buildMusicScore(
        staffLines: List<StaffLineGroup>,
        noteHeads: List<DetectedNoteHead>
    ): MusicScore {
        val staves = staffLines.mapIndexed { staffIndex, staffLine ->
            val staffNotes = noteHeads
                .filter { it.staffIndex == staffIndex }
                .mapIndexed { noteIndex, noteHead ->
                    convertToMusicNote(noteHead, noteIndex, staffLine)
                }

            // Group notes into measures (assume 4/4 time, 4 notes per measure for simplicity)
            val measures = staffNotes.chunked(4).mapIndexed { measureIndex, notes ->
                Measure(
                    number = measureIndex,
                    notes = notes.mapIndexed { idx, note ->
                        note.copy(
                            measureNumber = measureIndex,
                            positionInMeasure = idx.toFloat()
                        )
                    },
                    timeSignature = TimeSignature.COMMON_TIME,
                    keySignature = KeySignature.C_MAJOR
                )
            }

            Staff(
                clef = Clef.TREBLE,
                measures = measures.ifEmpty {
                    // Create at least one measure with a rest if no notes detected
                    listOf(
                        Measure(
                            number = 0,
                            notes = listOf(
                                MusicNote(
                                    pitch = Pitch.REST,
                                    octave = 4,
                                    duration = NoteDuration.WHOLE,
                                    positionInMeasure = 0f,
                                    measureNumber = 0,
                                    isRest = true
                                )
                            ),
                            timeSignature = TimeSignature.COMMON_TIME,
                            keySignature = KeySignature.C_MAJOR
                        )
                    )
                },
                initialTimeSignature = TimeSignature.COMMON_TIME,
                initialKeySignature = KeySignature.C_MAJOR
            )
        }

        return MusicScore(
            title = "Scanned Music",
            composer = "Unknown",
            tempo = 120,
            staves = staves.ifEmpty {
                listOf(createDefaultStaff())
            }
        )
    }

    /**
     * Convert detected note head to MusicNote
     */
    private fun convertToMusicNote(
        noteHead: DetectedNoteHead,
        index: Int,
        staffLine: StaffLineGroup
    ): MusicNote {
        // Convert staff position to pitch (assuming treble clef)
        // Staff position 0 = B4 (middle line of treble clef)
        val (pitch, octave) = staffPositionToPitch(noteHead.staffPosition, Clef.TREBLE)

        // Determine duration based on whether note is filled
        val duration = if (noteHead.isFilled) {
            NoteDuration.QUARTER
        } else {
            NoteDuration.HALF
        }

        return MusicNote(
            pitch = pitch,
            octave = octave,
            duration = duration,
            positionInMeasure = (index % 4).toFloat(),
            measureNumber = index / 4,
            isRest = false
        )
    }

    /**
     * Convert staff position to pitch and octave
     * Position 0 = middle line
     */
    private fun staffPositionToPitch(position: Int, clef: Clef): Pair<Pitch, Int> {
        // For treble clef: middle line (position 0) = B4
        // Each position is a diatonic step
        val trebleClefPitches = listOf(
            Pitch.E to 3,  // -6
            Pitch.F to 3,  // -5
            Pitch.G to 3,  // -4
            Pitch.A to 3,  // -3
            Pitch.B to 3,  // -2
            Pitch.C to 4,  // -1
            Pitch.D to 4,  // 0 (first line)
            Pitch.E to 4,  // 1
            Pitch.F to 4,  // 2
            Pitch.G to 4,  // 3
            Pitch.A to 4,  // 4
            Pitch.B to 4,  // 5 (middle line)
            Pitch.C to 5,  // 6
            Pitch.D to 5,  // 7
            Pitch.E to 5,  // 8
            Pitch.F to 5,  // 9
            Pitch.G to 5,  // 10
            Pitch.A to 5,  // 11
            Pitch.B to 5,  // 12
        )

        // Offset to map position to array index
        val index = (position + 6).coerceIn(0, trebleClefPitches.lastIndex)
        return trebleClefPitches[index]
    }

    /**
     * Create a default staff with a simple melody for demo purposes
     */
    private fun createDefaultStaff(): Staff {
        return Staff(
            clef = Clef.TREBLE,
            measures = listOf(createDemoMeasure(0)),
            initialTimeSignature = TimeSignature.COMMON_TIME,
            initialKeySignature = KeySignature.C_MAJOR
        )
    }

    private fun createDemoMeasure(number: Int): Measure {
        return Measure(
            number = number,
            notes = listOf(
                MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, number),
                MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 1f, number),
                MusicNote(Pitch.G, 4, NoteDuration.QUARTER, 2f, number),
                MusicNote(Pitch.C, 5, NoteDuration.QUARTER, 3f, number),
            ),
            timeSignature = TimeSignature.COMMON_TIME,
            keySignature = KeySignature.C_MAJOR
        )
    }

    /**
     * Generate a demo score when no music is detected
     * This helps demonstrate the playback functionality
     */
    private fun generateDemoScore(): MusicScore {
        val notes = listOf(
            // "Twinkle Twinkle Little Star" - first phrase
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 0f, 0),
            MusicNote(Pitch.C, 4, NoteDuration.QUARTER, 1f, 0),
            MusicNote(Pitch.G, 4, NoteDuration.QUARTER, 2f, 0),
            MusicNote(Pitch.G, 4, NoteDuration.QUARTER, 3f, 0),

            MusicNote(Pitch.A, 4, NoteDuration.QUARTER, 0f, 1),
            MusicNote(Pitch.A, 4, NoteDuration.QUARTER, 1f, 1),
            MusicNote(Pitch.G, 4, NoteDuration.HALF, 2f, 1),

            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 0f, 2),
            MusicNote(Pitch.F, 4, NoteDuration.QUARTER, 1f, 2),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 2f, 2),
            MusicNote(Pitch.E, 4, NoteDuration.QUARTER, 3f, 2),

            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 0f, 3),
            MusicNote(Pitch.D, 4, NoteDuration.QUARTER, 1f, 3),
            MusicNote(Pitch.C, 4, NoteDuration.HALF, 2f, 3),
        )

        val measures = (0..3).map { measureNum ->
            Measure(
                number = measureNum,
                notes = notes.filter { it.measureNumber == measureNum },
                timeSignature = TimeSignature.COMMON_TIME,
                keySignature = KeySignature.C_MAJOR
            )
        }

        return MusicScore(
            title = "Demo - Twinkle Twinkle",
            composer = "Demo",
            tempo = 100,
            staves = listOf(
                Staff(
                    clef = Clef.TREBLE,
                    measures = measures,
                    initialTimeSignature = TimeSignature.COMMON_TIME,
                    initialKeySignature = KeySignature.C_MAJOR
                )
            )
        )
    }
}

class RecognitionException(message: String) : Exception(message)
