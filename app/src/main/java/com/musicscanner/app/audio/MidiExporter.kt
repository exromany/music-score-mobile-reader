package com.musicscanner.app.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.musicscanner.app.data.MusicScore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Exports MusicScore to Standard MIDI File format
 */
object MidiExporter {

    private const val TICKS_PER_QUARTER = 480
    private const val DEFAULT_TEMPO = 500000 // microseconds per quarter note (120 BPM)

    /**
     * Export a MusicScore to a MIDI file and return the file URI
     */
    fun exportToFile(context: Context, score: MusicScore): Uri? {
        val filename = sanitizeFilename(score.title.ifEmpty { "score" }) + ".mid"
        val cacheDir = File(context.cacheDir, "midi_exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, filename)

        return try {
            FileOutputStream(file).use { fos ->
                writeMidiFile(fos, score)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create a share intent for the MIDI file
     */
    fun createShareIntent(context: Context, score: MusicScore): Intent? {
        val uri = exportToFile(context, score) ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/midi"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, score.title.ifEmpty { "Music Score" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeMidiFile(output: OutputStream, score: MusicScore) {
        val notes = score.getAllNotes()
        val tempo = score.tempo

        // Calculate tempo in microseconds per quarter note
        val tempoMicros = (60_000_000 / tempo)

        // Build track data
        val trackData = mutableListOf<Byte>()

        // Tempo meta event
        trackData.addAll(writeVarLen(0)) // Delta time
        trackData.add(0xFF.toByte()) // Meta event
        trackData.add(0x51.toByte()) // Tempo
        trackData.add(0x03.toByte()) // Length
        trackData.add((tempoMicros shr 16 and 0xFF).toByte())
        trackData.add((tempoMicros shr 8 and 0xFF).toByte())
        trackData.add((tempoMicros and 0xFF).toByte())

        // Time signature meta event (4/4)
        trackData.addAll(writeVarLen(0))
        trackData.add(0xFF.toByte())
        trackData.add(0x58.toByte())
        trackData.add(0x04.toByte())
        trackData.add(0x04.toByte()) // Numerator
        trackData.add(0x02.toByte()) // Denominator (2^2 = 4)
        trackData.add(0x18.toByte()) // Clocks per click
        trackData.add(0x08.toByte()) // 32nd notes per quarter

        // Track name
        val trackName = score.title.ifEmpty { "Music Score" }.toByteArray(Charsets.US_ASCII)
        trackData.addAll(writeVarLen(0))
        trackData.add(0xFF.toByte())
        trackData.add(0x03.toByte())
        trackData.addAll(writeVarLen(trackName.size))
        trackData.addAll(trackName.toList())

        // Convert notes to MIDI events
        var currentTick = 0L

        for (note in notes) {
            val midiNote = note.toMidiNoteNumber()
            if (midiNote < 0) {
                // Rest - just advance time
                val durationTicks = (note.getDurationInBeats() * TICKS_PER_QUARTER).toInt()
                currentTick += durationTicks
                continue
            }

            val durationTicks = (note.getDurationInBeats() * TICKS_PER_QUARTER).toInt()
            val noteTick = ((note.measureNumber * 4 + note.positionInMeasure) * TICKS_PER_QUARTER).toLong()

            // Delta time to note start
            val deltaOn = (noteTick - currentTick).coerceAtLeast(0)
            currentTick = noteTick

            // Note On
            trackData.addAll(writeVarLen(deltaOn.toInt()))
            trackData.add(0x90.toByte()) // Note on, channel 0
            trackData.add(midiNote.toByte())
            trackData.add(100.toByte()) // Velocity

            // Note Off (after duration)
            trackData.addAll(writeVarLen(durationTicks))
            trackData.add(0x80.toByte()) // Note off, channel 0
            trackData.add(midiNote.toByte())
            trackData.add(0.toByte()) // Velocity

            currentTick += durationTicks
        }

        // End of track
        trackData.addAll(writeVarLen(0))
        trackData.add(0xFF.toByte())
        trackData.add(0x2F.toByte())
        trackData.add(0x00.toByte())

        // Write header chunk
        output.write("MThd".toByteArray()) // Chunk type
        output.write(intToBytes(6)) // Chunk length
        output.write(shortToBytes(0)) // Format type 0
        output.write(shortToBytes(1)) // Number of tracks
        output.write(shortToBytes(TICKS_PER_QUARTER)) // Ticks per quarter note

        // Write track chunk
        output.write("MTrk".toByteArray())
        output.write(intToBytes(trackData.size))
        output.write(trackData.toByteArray())
    }

    private fun writeVarLen(value: Int): List<Byte> {
        if (value < 0) return listOf(0)

        val result = mutableListOf<Byte>()
        var v = value

        result.add(0, (v and 0x7F).toByte())
        v = v shr 7

        while (v > 0) {
            result.add(0, ((v and 0x7F) or 0x80).toByte())
            v = v shr 7
        }

        return result
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
    }
}
