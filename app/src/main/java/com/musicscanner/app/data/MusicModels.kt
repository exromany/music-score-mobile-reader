package com.musicscanner.app.data

/**
 * Represents a musical note with all its properties
 */
data class MusicNote(
    val pitch: Pitch,
    val octave: Int,
    val duration: NoteDuration,
    val positionInMeasure: Float, // Position within the measure (0.0 to measure length)
    val measureNumber: Int,
    val isRest: Boolean = false,
    val accidental: Accidental = Accidental.NONE,
    val isDotted: Boolean = false,
    val tieStart: Boolean = false,
    val tieEnd: Boolean = false
) {
    /**
     * Get MIDI note number (0-127)
     * Middle C (C4) = 60
     */
    fun toMidiNoteNumber(): Int {
        if (isRest) return -1
        val baseMidiNote = when (pitch) {
            Pitch.C -> 0
            Pitch.D -> 2
            Pitch.E -> 4
            Pitch.F -> 5
            Pitch.G -> 7
            Pitch.A -> 9
            Pitch.B -> 11
            Pitch.REST -> return -1
        }
        val accidentalOffset = when (accidental) {
            Accidental.SHARP -> 1
            Accidental.FLAT -> -1
            Accidental.NATURAL, Accidental.NONE -> 0
            Accidental.DOUBLE_SHARP -> 2
            Accidental.DOUBLE_FLAT -> -2
        }
        return (octave + 1) * 12 + baseMidiNote + accidentalOffset
    }

    /**
     * Get duration in beats
     */
    fun getDurationInBeats(): Float {
        val baseDuration = when (duration) {
            NoteDuration.WHOLE -> 4.0f
            NoteDuration.HALF -> 2.0f
            NoteDuration.QUARTER -> 1.0f
            NoteDuration.EIGHTH -> 0.5f
            NoteDuration.SIXTEENTH -> 0.25f
            NoteDuration.THIRTY_SECOND -> 0.125f
        }
        return if (isDotted) baseDuration * 1.5f else baseDuration
    }
}

enum class Pitch {
    C, D, E, F, G, A, B, REST
}

enum class NoteDuration {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH,
    THIRTY_SECOND
}

enum class Accidental {
    NONE,
    SHARP,
    FLAT,
    NATURAL,
    DOUBLE_SHARP,
    DOUBLE_FLAT
}

/**
 * Represents a clef type
 */
enum class Clef {
    TREBLE,  // G clef
    BASS,    // F clef
    ALTO,    // C clef on middle line
    TENOR    // C clef on fourth line
}

/**
 * Time signature
 */
data class TimeSignature(
    val numerator: Int,    // Beats per measure
    val denominator: Int   // Note value that gets one beat
) {
    companion object {
        val COMMON_TIME = TimeSignature(4, 4)
        val CUT_TIME = TimeSignature(2, 2)
        val WALTZ_TIME = TimeSignature(3, 4)
    }
}

/**
 * Key signature represented by number of sharps (positive) or flats (negative)
 */
data class KeySignature(
    val accidentals: Int  // Positive for sharps, negative for flats
) {
    companion object {
        val C_MAJOR = KeySignature(0)
        val G_MAJOR = KeySignature(1)
        val D_MAJOR = KeySignature(2)
        val F_MAJOR = KeySignature(-1)
        val Bb_MAJOR = KeySignature(-2)
    }

    fun getAffectedPitches(): Map<Pitch, Accidental> {
        val sharpOrder = listOf(Pitch.F, Pitch.C, Pitch.G, Pitch.D, Pitch.A, Pitch.E, Pitch.B)
        val flatOrder = listOf(Pitch.B, Pitch.E, Pitch.A, Pitch.D, Pitch.G, Pitch.C, Pitch.F)

        return if (accidentals > 0) {
            sharpOrder.take(accidentals).associateWith { Accidental.SHARP }
        } else if (accidentals < 0) {
            flatOrder.take(-accidentals).associateWith { Accidental.FLAT }
        } else {
            emptyMap()
        }
    }
}

/**
 * A measure containing notes
 */
data class Measure(
    val number: Int,
    val notes: List<MusicNote>,
    val timeSignature: TimeSignature,
    val keySignature: KeySignature
)

/**
 * A staff (single line of music)
 */
data class Staff(
    val clef: Clef,
    val measures: List<Measure>,
    val initialTimeSignature: TimeSignature,
    val initialKeySignature: KeySignature
)

/**
 * Complete parsed music score
 */
data class MusicScore(
    val title: String = "",
    val composer: String = "",
    val tempo: Int = 120, // BPM
    val staves: List<Staff>
) {
    /**
     * Get all notes flattened into a single list, sorted by time
     */
    fun getAllNotes(): List<MusicNote> {
        return staves.flatMap { staff ->
            staff.measures.flatMap { measure ->
                measure.notes
            }
        }.sortedWith(compareBy({ it.measureNumber }, { it.positionInMeasure }))
    }

    /**
     * Calculate total duration in beats
     */
    fun getTotalBeats(): Float {
        val lastNote = getAllNotes().lastOrNull() ?: return 0f
        return lastNote.measureNumber * 4f + lastNote.positionInMeasure + lastNote.getDurationInBeats()
    }
}

/**
 * Represents the current playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentBeat: Float = 0f,
    val totalBeats: Float = 0f,
    val tempo: Int = 120,
    val currentNoteIndex: Int = 0
) {
    val progress: Float
        get() = if (totalBeats > 0) currentBeat / totalBeats else 0f
}

/**
 * Processing state for the OMR pipeline
 */
sealed class ProcessingState {
    object Idle : ProcessingState()
    object LoadingImage : ProcessingState()
    object PreprocessingImage : ProcessingState()
    object DetectingStaffLines : ProcessingState()
    object RecognizingNotes : ProcessingState()
    object GeneratingMidi : ProcessingState()
    data class Complete(val score: MusicScore) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
