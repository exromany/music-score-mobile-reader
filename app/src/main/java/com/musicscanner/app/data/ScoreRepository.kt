package com.musicscanner.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for saving and loading score history using SharedPreferences
 */
class ScoreRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Save a score to history
     */
    fun saveScore(score: MusicScore, imagePath: String? = null): String {
        val id = System.currentTimeMillis().toString()
        val entry = ScoreHistoryEntry(
            id = id,
            title = score.title.ifEmpty { "Untitled Score" },
            composer = score.composer,
            tempo = score.tempo,
            noteCount = score.getAllNotes().size,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath,
            scoreData = serializeScore(score)
        )

        val history = getHistoryEntries().toMutableList()
        history.add(0, entry) // Add to beginning

        // Keep only last 50 scores
        val trimmed = history.take(MAX_HISTORY_SIZE)
        saveHistoryEntries(trimmed)

        return id
    }

    /**
     * Get all history entries (metadata only)
     */
    fun getHistoryEntries(): List<ScoreHistoryEntry> {
        val entriesJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ScoreHistoryEntry>>(entriesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Load a full score by ID
     */
    fun loadScore(id: String): MusicScore? {
        val entry = getHistoryEntries().find { it.id == id } ?: return null
        return deserializeScore(entry.scoreData)
    }

    /**
     * Delete a score from history
     */
    fun deleteScore(id: String) {
        val history = getHistoryEntries().filterNot { it.id == id }
        saveHistoryEntries(history)
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistoryEntries(entries: List<ScoreHistoryEntry>) {
        val entriesJson = json.encodeToString(entries)
        prefs.edit().putString(KEY_HISTORY, entriesJson).apply()
    }

    private fun serializeScore(score: MusicScore): String {
        val serializable = score.toSerializable()
        return json.encodeToString(serializable)
    }

    private fun deserializeScore(data: String): MusicScore? {
        return try {
            val serializable = json.decodeFromString<SerializableMusicScore>(data)
            serializable.toMusicScore()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "music_scanner_history"
        private const val KEY_HISTORY = "score_history"
        private const val MAX_HISTORY_SIZE = 50
    }
}

/**
 * History entry stored in SharedPreferences
 */
@Serializable
data class ScoreHistoryEntry(
    val id: String,
    val title: String,
    val composer: String,
    val tempo: Int,
    val noteCount: Int,
    val timestamp: Long,
    val imagePath: String?,
    val scoreData: String
)

/**
 * Serializable version of MusicScore for JSON storage
 */
@Serializable
data class SerializableMusicScore(
    val title: String,
    val composer: String,
    val tempo: Int,
    val staves: List<SerializableStaff>
) {
    fun toMusicScore(): MusicScore {
        return MusicScore(
            title = title,
            composer = composer,
            tempo = tempo,
            staves = staves.map { it.toStaff() }
        )
    }
}

@Serializable
data class SerializableStaff(
    val clef: String,
    val measures: List<SerializableMeasure>,
    val timeSignatureNum: Int,
    val timeSignatureDen: Int,
    val keyAccidentals: Int
) {
    fun toStaff(): Staff {
        return Staff(
            clef = Clef.valueOf(clef),
            measures = measures.map { it.toMeasure() },
            initialTimeSignature = TimeSignature(timeSignatureNum, timeSignatureDen),
            initialKeySignature = KeySignature(keyAccidentals)
        )
    }
}

@Serializable
data class SerializableMeasure(
    val number: Int,
    val notes: List<SerializableNote>,
    val timeSignatureNum: Int,
    val timeSignatureDen: Int,
    val keyAccidentals: Int
) {
    fun toMeasure(): Measure {
        return Measure(
            number = number,
            notes = notes.map { it.toMusicNote() },
            timeSignature = TimeSignature(timeSignatureNum, timeSignatureDen),
            keySignature = KeySignature(keyAccidentals)
        )
    }
}

@Serializable
data class SerializableNote(
    val pitch: String,
    val octave: Int,
    val duration: String,
    val positionInMeasure: Float,
    val measureNumber: Int,
    val isRest: Boolean,
    val accidental: String,
    val isDotted: Boolean,
    val tieStart: Boolean,
    val tieEnd: Boolean
) {
    fun toMusicNote(): MusicNote {
        return MusicNote(
            pitch = Pitch.valueOf(pitch),
            octave = octave,
            duration = NoteDuration.valueOf(duration),
            positionInMeasure = positionInMeasure,
            measureNumber = measureNumber,
            isRest = isRest,
            accidental = Accidental.valueOf(accidental),
            isDotted = isDotted,
            tieStart = tieStart,
            tieEnd = tieEnd
        )
    }
}

// Extension functions for converting to serializable versions
fun MusicScore.toSerializable(): SerializableMusicScore {
    return SerializableMusicScore(
        title = title,
        composer = composer,
        tempo = tempo,
        staves = staves.map { it.toSerializable() }
    )
}

fun Staff.toSerializable(): SerializableStaff {
    return SerializableStaff(
        clef = clef.name,
        measures = measures.map { it.toSerializable() },
        timeSignatureNum = initialTimeSignature.numerator,
        timeSignatureDen = initialTimeSignature.denominator,
        keyAccidentals = initialKeySignature.accidentals
    )
}

fun Measure.toSerializable(): SerializableMeasure {
    return SerializableMeasure(
        number = number,
        notes = notes.map { it.toSerializable() },
        timeSignatureNum = timeSignature.numerator,
        timeSignatureDen = timeSignature.denominator,
        keyAccidentals = keySignature.accidentals
    )
}

fun MusicNote.toSerializable(): SerializableNote {
    return SerializableNote(
        pitch = pitch.name,
        octave = octave,
        duration = duration.name,
        positionInMeasure = positionInMeasure,
        measureNumber = measureNumber,
        isRest = isRest,
        accidental = accidental.name,
        isDotted = isDotted,
        tieStart = tieStart,
        tieEnd = tieEnd
    )
}
