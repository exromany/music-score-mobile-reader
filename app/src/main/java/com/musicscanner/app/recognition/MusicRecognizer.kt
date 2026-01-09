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
    private val imageEnhancer = ImageEnhancer()

    /**
     * Settings for image preprocessing
     */
    data class PreprocessingSettings(
        val autoCrop: Boolean = true,
        val correctPerspective: Boolean = true,
        val enhanceContrast: Boolean = true
    )

    /**
     * Process an image and extract music notation
     */
    suspend fun recognize(
        imagePath: String,
        onProgressUpdate: (ProcessingState) -> Unit,
        preprocessingSettings: PreprocessingSettings = PreprocessingSettings()
    ): MusicScore = withContext(Dispatchers.Default) {

        try {
            // Step 1: Load image
            onProgressUpdate(ProcessingState.LoadingImage)
            var bitmap = loadImage(imagePath)
                ?: throw RecognitionException("Failed to load image")

            // Step 2: Preprocess image with auto-crop and perspective correction
            onProgressUpdate(ProcessingState.PreprocessingImage)

            // Apply auto-crop if enabled
            if (preprocessingSettings.autoCrop) {
                val cropResult = imageEnhancer.detectCropBounds(bitmap)
                if (cropResult.isValid) {
                    bitmap = imageEnhancer.applyCrop(bitmap, cropResult)
                }
            }

            // Apply perspective correction if enabled
            if (preprocessingSettings.correctPerspective) {
                val perspectiveResult = imageEnhancer.detectPerspective(bitmap)
                if (perspectiveResult.needsCorrection) {
                    bitmap = imageEnhancer.correctPerspective(bitmap, perspectiveResult)
                }
            }

            // Enhance for better recognition
            if (preprocessingSettings.enhanceContrast) {
                bitmap = imageEnhancer.enhanceForRecognition(bitmap)
            }

            val grayscale = imageProcessor.toGrayscale(bitmap)
            val binary = imageProcessor.binarize(grayscale, bitmap.width, bitmap.height)

            // Step 3: Detect staff lines
            onProgressUpdate(ProcessingState.DetectingStaffLines)
            val staffLines = imageProcessor.detectStaffLines(binary, bitmap.width, bitmap.height)

            if (staffLines.isEmpty()) {
                // If no staff lines detected, generate a simple demo score
                return@withContext generateDemoScore()
            }

            // Step 4: Detect clefs, time signatures, and key signatures for each staff
            onProgressUpdate(ProcessingState.RecognizingNotes)
            val clefs = staffLines.map { staff ->
                imageProcessor.detectClef(binary, bitmap.width, bitmap.height, staff)
            }

            // Update staff groups with detected clefs
            staffLines.forEachIndexed { index, staff ->
                staff.detectedClef = clefs[index].clef
            }

            // Detect time and key signatures
            val timeSignatures = staffLines.mapIndexed { index, staff ->
                val clefEndX = clefs[index].endX
                imageProcessor.detectTimeSignature(binary, bitmap.width, bitmap.height, staff, clefEndX)
            }

            val keySignatures = staffLines.mapIndexed { index, staff ->
                val timeEndX = timeSignatures[index].endX
                imageProcessor.detectKeySignature(binary, bitmap.width, bitmap.height, staff, timeEndX)
            }

            // Update staff groups with detected signatures
            staffLines.forEachIndexed { index, staff ->
                staff.detectedTimeSignature = timeSignatures[index].timeSignature
                staff.detectedKeySignature = keySignatures[index].keySignature
            }

            // Detect grand staff pairings
            val grandStaffs = imageProcessor.detectGrandStaff(staffLines, clefs)

            // Step 5: Detect note heads
            val noteHeads = imageProcessor.detectNoteHeads(
                binary, bitmap.width, bitmap.height, staffLines
            )

            // Step 6: Detect note durations (stems, flags, beams)
            val noteDurations = noteHeads.map { noteHead ->
                val staffIndex = noteHead.staffIndex
                if (staffIndex in staffLines.indices) {
                    imageProcessor.detectNoteDuration(
                        binary, bitmap.width, bitmap.height, noteHead, staffLines[staffIndex]
                    )
                } else {
                    NoteDuration.QUARTER
                }
            }

            // Step 7: Detect beams for grouped notes
            val allBeams = staffLines.mapIndexed { staffIndex, staff ->
                val staffNotes = noteHeads.filter { it.staffIndex == staffIndex }
                imageProcessor.detectBeams(binary, bitmap.width, bitmap.height, staffNotes, staff)
            }

            // Step 8: Detect chords
            val allChords = staffLines.mapIndexed { staffIndex, staff ->
                val staffNotes = noteHeads.filter { it.staffIndex == staffIndex }
                imageProcessor.detectChords(staffNotes, staff)
            }

            // Step 9: Detect rests
            val allRests = staffLines.mapIndexed { staffIndex, staff ->
                val staffNotes = noteHeads.filter { it.staffIndex == staffIndex }
                imageProcessor.detectRests(binary, bitmap.width, bitmap.height, staff, staffNotes)
            }

            // Step 10: Build music score
            onProgressUpdate(ProcessingState.GeneratingMidi)
            val score = buildMusicScore(
                staffLines, noteHeads, noteDurations, allRests, allChords, allBeams, grandStaffs
            )

            onProgressUpdate(ProcessingState.Complete(score))
            score

        } catch (e: Exception) {
            onProgressUpdate(ProcessingState.Error(e.message ?: "Unknown error"))
            throw e
        }
    }

    /**
     * Process multiple pages and merge into a single score
     */
    suspend fun recognizeMultiPage(
        imagePaths: List<String>,
        onProgressUpdate: (ProcessingState) -> Unit,
        onPageProcessed: (Int, Int, ScannedPage) -> Unit,
        preprocessingSettings: PreprocessingSettings = PreprocessingSettings()
    ): MusicScore = withContext(Dispatchers.Default) {
        val pages = mutableListOf<ScannedPage>()
        val scores = mutableListOf<MusicScore>()

        for ((index, imagePath) in imagePaths.withIndex()) {
            val pageNumber = index + 1
            onPageProcessed(pageNumber, imagePaths.size, ScannedPage(pageNumber, imagePath, null, PageStatus.PROCESSING))

            try {
                val score = recognize(imagePath, onProgressUpdate, preprocessingSettings)
                val page = ScannedPage(pageNumber, imagePath, score, PageStatus.COMPLETED)
                pages.add(page)
                scores.add(score)
                onPageProcessed(pageNumber, imagePaths.size, page)
            } catch (e: Exception) {
                val errorPage = ScannedPage(pageNumber, imagePath, null, PageStatus.ERROR)
                pages.add(errorPage)
                onPageProcessed(pageNumber, imagePaths.size, errorPage)
            }
        }

        // Merge all scores into one
        mergeScores(scores)
    }

    /**
     * Merge multiple scores into a single continuous score
     */
    private fun mergeScores(scores: List<MusicScore>): MusicScore {
        if (scores.isEmpty()) {
            return generateDemoScore()
        }

        if (scores.size == 1) {
            return scores.first()
        }

        // Combine all staves from all scores
        val mergedStaves = mutableListOf<Staff>()
        var measureOffset = 0

        for (score in scores) {
            for (staff in score.staves) {
                // Offset measure numbers to create continuous sequence
                val offsetMeasures = staff.measures.map { measure ->
                    measure.copy(
                        number = measure.number + measureOffset,
                        notes = measure.notes.map { note ->
                            note.copy(measureNumber = note.measureNumber + measureOffset)
                        }
                    )
                }

                // Find or create matching staff in merged result
                val existingStaffIndex = mergedStaves.indexOfFirst { it.clef == staff.clef }
                if (existingStaffIndex >= 0) {
                    // Append measures to existing staff
                    val existingStaff = mergedStaves[existingStaffIndex]
                    mergedStaves[existingStaffIndex] = existingStaff.copy(
                        measures = existingStaff.measures + offsetMeasures
                    )
                } else {
                    // Add new staff with offset measures
                    mergedStaves.add(staff.copy(measures = offsetMeasures))
                }
            }

            // Update measure offset for next score
            val maxMeasure = score.staves.flatMap { it.measures }.maxOfOrNull { it.number } ?: 0
            measureOffset += maxMeasure + 1
        }

        return MusicScore(
            title = scores.firstOrNull()?.title ?: "Merged Score",
            composer = scores.firstOrNull()?.composer ?: "Unknown",
            tempo = scores.firstOrNull()?.tempo ?: 120,
            staves = mergedStaves
        )
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
        noteHeads: List<DetectedNoteHead>,
        noteDurations: List<NoteDuration>,
        allRests: List<List<DetectedRest>>,
        allChords: List<List<DetectedChord>>,
        allBeams: List<List<DetectedBeam>>,
        grandStaffs: List<GrandStaffGroup>
    ): MusicScore {
        val staves = staffLines.mapIndexed { staffIndex, staffLine ->
            val staffNoteHeads = noteHeads.withIndex()
                .filter { it.value.staffIndex == staffIndex }

            val staffRests = allRests.getOrNull(staffIndex) ?: emptyList()
            val staffChords = allChords.getOrNull(staffIndex) ?: emptyList()
            val staffBeams = allBeams.getOrNull(staffIndex) ?: emptyList()

            // Get detected time and key signatures
            val timeSignature = staffLine.detectedTimeSignature
            val keySignature = staffLine.detectedKeySignature
            val clef = staffLine.detectedClef

            // Build list of all musical events (notes and rests) sorted by x position
            val musicalEvents = mutableListOf<MusicalEvent>()

            // Add notes with their detected durations
            for ((globalIndex, noteHead) in staffNoteHeads) {
                val duration = noteDurations.getOrNull(globalIndex) ?: NoteDuration.QUARTER
                musicalEvents.add(MusicalEvent.NoteEvent(noteHead, duration, globalIndex))
            }

            // Add rests
            for (rest in staffRests) {
                musicalEvents.add(MusicalEvent.RestEvent(rest))
            }

            // Sort events by x position
            musicalEvents.sortBy {
                when (it) {
                    is MusicalEvent.NoteEvent -> it.noteHead.x
                    is MusicalEvent.RestEvent -> it.rest.x
                }
            }

            // Handle chords - merge notes that are part of the same chord
            val chordNoteIndices = staffChords.flatMap { it.noteIndices }.toSet()

            // Calculate beats per measure from time signature
            val beatsPerMeasure = timeSignature.numerator.toFloat()

            // Build notes from events, tracking position
            val staffNotes = mutableListOf<MusicNote>()
            var currentBeat = 0f
            var currentMeasure = 0

            for (event in musicalEvents) {
                when (event) {
                    is MusicalEvent.NoteEvent -> {
                        val (pitch, octave) = staffPositionToPitch(event.noteHead.staffPosition, clef)

                        // Apply key signature accidentals
                        val keyAccidentals = keySignature.getAffectedPitches()
                        val accidental = keyAccidentals[pitch] ?: Accidental.NONE

                        val note = MusicNote(
                            pitch = pitch,
                            octave = octave,
                            duration = event.duration,
                            positionInMeasure = currentBeat,
                            measureNumber = currentMeasure,
                            isRest = false,
                            accidental = accidental
                        )
                        staffNotes.add(note)

                        // Check if this note is part of a chord - if so, don't advance beat
                        val isChordNote = event.globalIndex in chordNoteIndices
                        if (!isChordNote) {
                            currentBeat += note.getDurationInBeats()
                            if (currentBeat >= beatsPerMeasure) {
                                currentMeasure++
                                currentBeat -= beatsPerMeasure
                            }
                        }
                    }
                    is MusicalEvent.RestEvent -> {
                        val rest = MusicNote(
                            pitch = Pitch.REST,
                            octave = 4,
                            duration = event.rest.duration,
                            positionInMeasure = currentBeat,
                            measureNumber = currentMeasure,
                            isRest = true
                        )
                        staffNotes.add(rest)

                        currentBeat += rest.getDurationInBeats()
                        if (currentBeat >= beatsPerMeasure) {
                            currentMeasure++
                            currentBeat -= beatsPerMeasure
                        }
                    }
                }
            }

            // Group notes into measures
            val measureCount = (staffNotes.maxOfOrNull { it.measureNumber } ?: 0) + 1
            val measures = (0 until measureCount).map { measureNum ->
                Measure(
                    number = measureNum,
                    notes = staffNotes.filter { it.measureNumber == measureNum },
                    timeSignature = timeSignature,
                    keySignature = keySignature
                )
            }

            Staff(
                clef = clef,
                measures = measures.ifEmpty {
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
                            timeSignature = timeSignature,
                            keySignature = keySignature
                        )
                    )
                },
                initialTimeSignature = timeSignature,
                initialKeySignature = keySignature
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
     * Helper sealed class for ordering musical events
     */
    private sealed class MusicalEvent {
        data class NoteEvent(
            val noteHead: DetectedNoteHead,
            val duration: NoteDuration,
            val globalIndex: Int
        ) : MusicalEvent()

        data class RestEvent(val rest: DetectedRest) : MusicalEvent()
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
     * Position 0 = middle line (B4 for treble, D3 for bass)
     */
    private fun staffPositionToPitch(position: Int, clef: Clef): Pair<Pitch, Int> {
        return when (clef) {
            Clef.TREBLE -> trebleClefPositionToPitch(position)
            Clef.BASS -> bassClefPositionToPitch(position)
            Clef.ALTO -> altoClefPositionToPitch(position)
            Clef.TENOR -> tenorClefPositionToPitch(position)
        }
    }

    /**
     * Treble clef: middle line (position 0) = B4
     */
    private fun trebleClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
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
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    /**
     * Bass clef: middle line (position 0) = D3
     * Bass clef F line (4th line) = F2
     */
    private fun bassClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.G to 1,  // -6
            Pitch.A to 1,  // -5
            Pitch.B to 1,  // -4
            Pitch.C to 2,  // -3
            Pitch.D to 2,  // -2
            Pitch.E to 2,  // -1
            Pitch.F to 2,  // 0 (first line)
            Pitch.G to 2,  // 1
            Pitch.A to 2,  // 2 (fourth line - F clef marker)
            Pitch.B to 2,  // 3
            Pitch.C to 3,  // 4
            Pitch.D to 3,  // 5 (middle line)
            Pitch.E to 3,  // 6
            Pitch.F to 3,  // 7
            Pitch.G to 3,  // 8
            Pitch.A to 3,  // 9
            Pitch.B to 3,  // 10
            Pitch.C to 4,  // 11
            Pitch.D to 4,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    /**
     * Alto clef: middle line (position 0) = C4 (middle C)
     */
    private fun altoClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.F to 2,  // -6
            Pitch.G to 2,  // -5
            Pitch.A to 2,  // -4
            Pitch.B to 2,  // -3
            Pitch.C to 3,  // -2
            Pitch.D to 3,  // -1
            Pitch.E to 3,  // 0 (first line)
            Pitch.F to 3,  // 1
            Pitch.G to 3,  // 2
            Pitch.A to 3,  // 3
            Pitch.B to 3,  // 4
            Pitch.C to 4,  // 5 (middle line - middle C)
            Pitch.D to 4,  // 6
            Pitch.E to 4,  // 7
            Pitch.F to 4,  // 8
            Pitch.G to 4,  // 9
            Pitch.A to 4,  // 10
            Pitch.B to 4,  // 11
            Pitch.C to 5,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
    }

    /**
     * Tenor clef: middle line (position 0) = A3
     * C4 is on the fourth line
     */
    private fun tenorClefPositionToPitch(position: Int): Pair<Pitch, Int> {
        val pitches = listOf(
            Pitch.D to 2,  // -6
            Pitch.E to 2,  // -5
            Pitch.F to 2,  // -4
            Pitch.G to 2,  // -3
            Pitch.A to 2,  // -2
            Pitch.B to 2,  // -1
            Pitch.C to 3,  // 0 (first line)
            Pitch.D to 3,  // 1
            Pitch.E to 3,  // 2
            Pitch.F to 3,  // 3
            Pitch.G to 3,  // 4
            Pitch.A to 3,  // 5 (middle line)
            Pitch.B to 3,  // 6
            Pitch.C to 4,  // 7 (fourth line - middle C)
            Pitch.D to 4,  // 8
            Pitch.E to 4,  // 9
            Pitch.F to 4,  // 10
            Pitch.G to 4,  // 11
            Pitch.A to 4,  // 12
        )
        val index = (position + 6).coerceIn(0, pitches.lastIndex)
        return pitches[index]
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

/**
 * Represents a page in a multi-page scan
 */
data class ScannedPage(
    val pageNumber: Int,
    val imagePath: String,
    val score: MusicScore? = null,
    val status: PageStatus = PageStatus.PENDING
)

enum class PageStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    ERROR
}

/**
 * Result of multi-page recognition
 */
data class MultiPageResult(
    val pages: List<ScannedPage>,
    val mergedScore: MusicScore?,
    val totalPages: Int,
    val processedPages: Int
)
