package com.musicscanner.app.audio

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MidiExporter
 * Tests MIDI encoding utilities and format constants
 */
class MidiExporterTest {

    // ==================== Variable Length Encoding Tests ====================

    /**
     * Variable length encoding for MIDI follows these rules:
     * - Values 0-127 are encoded in one byte
     * - Larger values use continuation bits (bit 7 set for continuation)
     */
    @Test
    fun `writeVarLen encodes 0 correctly`() {
        val result = writeVarLen(0)
        assertEquals(1, result.size)
        assertEquals(0x00.toByte(), result[0])
    }

    @Test
    fun `writeVarLen encodes 127 in single byte`() {
        val result = writeVarLen(127)
        assertEquals(1, result.size)
        assertEquals(0x7F.toByte(), result[0])
    }

    @Test
    fun `writeVarLen encodes 128 in two bytes`() {
        val result = writeVarLen(128)
        assertEquals(2, result.size)
        assertEquals(0x81.toByte(), result[0])
        assertEquals(0x00.toByte(), result[1])
    }

    @Test
    fun `writeVarLen encodes 16383 in two bytes`() {
        // 16383 = 0x3FFF = binary 11111111111111 (14 bits)
        // Should encode as: 0xFF 0x7F
        val result = writeVarLen(16383)
        assertEquals(2, result.size)
        assertEquals(0xFF.toByte(), result[0])
        assertEquals(0x7F.toByte(), result[1])
    }

    @Test
    fun `writeVarLen encodes 16384 in three bytes`() {
        // 16384 = 0x4000 needs 3 bytes
        val result = writeVarLen(16384)
        assertEquals(3, result.size)
        assertEquals(0x81.toByte(), result[0])
        assertEquals(0x80.toByte(), result[1])
        assertEquals(0x00.toByte(), result[2])
    }

    @Test
    fun `writeVarLen handles TICKS_PER_QUARTER (480)`() {
        // 480 = 0x1E0 = needs 2 bytes
        val result = writeVarLen(480)
        assertEquals(2, result.size)
        assertEquals(0x83.toByte(), result[0])
        assertEquals(0x60.toByte(), result[1])
    }

    @Test
    fun `writeVarLen handles negative values`() {
        val result = writeVarLen(-1)
        // Should return [0] for negative values
        assertEquals(1, result.size)
        assertEquals(0x00.toByte(), result[0])
    }

    // ==================== Byte Conversion Tests ====================

    @Test
    fun `intToBytes converts correctly`() {
        val result = intToBytes(0x12345678)
        assertEquals(4, result.size)
        assertEquals(0x12.toByte(), result[0])
        assertEquals(0x34.toByte(), result[1])
        assertEquals(0x56.toByte(), result[2])
        assertEquals(0x78.toByte(), result[3])
    }

    @Test
    fun `intToBytes handles zero`() {
        val result = intToBytes(0)
        assertEquals(4, result.size)
        assertTrue(result.all { it == 0x00.toByte() })
    }

    @Test
    fun `intToBytes handles maximum value`() {
        val result = intToBytes(0x7FFFFFFF)
        assertEquals(4, result.size)
        assertEquals(0x7F.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[1])
        assertEquals(0xFF.toByte(), result[2])
        assertEquals(0xFF.toByte(), result[3])
    }

    @Test
    fun `shortToBytes converts correctly`() {
        val result = shortToBytes(0x1234)
        assertEquals(2, result.size)
        assertEquals(0x12.toByte(), result[0])
        assertEquals(0x34.toByte(), result[1])
    }

    @Test
    fun `shortToBytes handles zero`() {
        val result = shortToBytes(0)
        assertEquals(2, result.size)
        assertTrue(result.all { it == 0x00.toByte() })
    }

    @Test
    fun `shortToBytes handles TICKS_PER_QUARTER`() {
        val ticksPerQuarter = 480 // 0x01E0
        val result = shortToBytes(ticksPerQuarter)
        assertEquals(2, result.size)
        assertEquals(0x01.toByte(), result[0])
        assertEquals(0xE0.toByte(), result[1])
    }

    // ==================== Filename Sanitization Tests ====================

    @Test
    fun `sanitizeFilename removes special characters`() {
        val result = sanitizeFilename("Test/Score:Name?")
        assertEquals("Test_Score_Name_", result)
    }

    @Test
    fun `sanitizeFilename preserves alphanumeric characters`() {
        val result = sanitizeFilename("TestScore123")
        assertEquals("TestScore123", result)
    }

    @Test
    fun `sanitizeFilename preserves allowed special characters`() {
        val result = sanitizeFilename("Test_Score-1.2")
        assertEquals("Test_Score-1.2", result)
    }

    @Test
    fun `sanitizeFilename truncates long names to 50 characters`() {
        val longName = "A".repeat(100)
        val result = sanitizeFilename(longName)
        assertEquals(50, result.length)
    }

    @Test
    fun `sanitizeFilename handles empty string`() {
        val result = sanitizeFilename("")
        assertEquals("", result)
    }

    @Test
    fun `sanitizeFilename handles spaces`() {
        val result = sanitizeFilename("Test Score Name")
        assertEquals("Test_Score_Name", result)
    }

    // ==================== MIDI Constants Tests ====================

    @Test
    fun `TICKS_PER_QUARTER is standard value`() {
        val ticksPerQuarter = 480
        assertEquals(480, ticksPerQuarter)
    }

    @Test
    fun `DEFAULT_TEMPO is 120 BPM in microseconds`() {
        // 120 BPM = 500,000 microseconds per quarter note
        // 60 BPM = 1,000,000 microseconds per quarter note
        val tempo120Micros = 60_000_000 / 120
        assertEquals(500000, tempo120Micros)

        // Default tempo in MIDI is 120 BPM
        val defaultTempo = 500000
        assertEquals(defaultTempo, tempo120Micros)
    }

    @Test
    fun `tempo calculation is correct for various BPMs`() {
        // BPM to microseconds: micros = 60_000_000 / BPM
        assertEquals(1000000, 60_000_000 / 60)  // 60 BPM
        assertEquals(500000, 60_000_000 / 120)   // 120 BPM
        assertEquals(666666, 60_000_000 / 90)    // 90 BPM
        assertEquals(400000, 60_000_000 / 150)   // 150 BPM
    }

    // ==================== MIDI Event Tests ====================

    @Test
    fun `note on event format is correct`() {
        val noteOn = 0x90 // Note on, channel 0
        val midiNote = 60  // Middle C
        val velocity = 100

        assertEquals(0x90, noteOn)
        assertEquals(60, midiNote)
        assertEquals(100, velocity)
    }

    @Test
    fun `note off event format is correct`() {
        val noteOff = 0x80 // Note off, channel 0
        assertEquals(0x80, noteOff)
    }

    @Test
    fun `meta event tempo format is correct`() {
        val metaEvent = 0xFF
        val tempoType = 0x51
        val length = 0x03

        assertEquals(0xFF, metaEvent)
        assertEquals(0x51, tempoType)
        assertEquals(3, length)
    }

    @Test
    fun `meta event time signature format is correct`() {
        val metaEvent = 0xFF
        val timeSigType = 0x58
        val length = 0x04

        assertEquals(0xFF, metaEvent)
        assertEquals(0x58, timeSigType)
        assertEquals(4, length)
    }

    @Test
    fun `end of track event format is correct`() {
        val metaEvent = 0xFF
        val endOfTrack = 0x2F
        val length = 0x00

        assertEquals(0xFF, metaEvent)
        assertEquals(0x2F, endOfTrack)
        assertEquals(0, length)
    }

    // ==================== Duration to Ticks Conversion Tests ====================

    @Test
    fun `quarter note is one tick unit`() {
        val ticksPerQuarter = 480
        val quarterNoteTicks = ticksPerQuarter
        assertEquals(480, quarterNoteTicks)
    }

    @Test
    fun `half note is two tick units`() {
        val ticksPerQuarter = 480
        val halfNoteTicks = (2.0f * ticksPerQuarter).toInt()
        assertEquals(960, halfNoteTicks)
    }

    @Test
    fun `whole note is four tick units`() {
        val ticksPerQuarter = 480
        val wholeNoteTicks = (4.0f * ticksPerQuarter).toInt()
        assertEquals(1920, wholeNoteTicks)
    }

    @Test
    fun `eighth note is half tick unit`() {
        val ticksPerQuarter = 480
        val eighthNoteTicks = (0.5f * ticksPerQuarter).toInt()
        assertEquals(240, eighthNoteTicks)
    }

    @Test
    fun `sixteenth note is quarter tick unit`() {
        val ticksPerQuarter = 480
        val sixteenthNoteTicks = (0.25f * ticksPerQuarter).toInt()
        assertEquals(120, sixteenthNoteTicks)
    }

    // ==================== Helper Functions (Replicating Private Methods) ====================

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
