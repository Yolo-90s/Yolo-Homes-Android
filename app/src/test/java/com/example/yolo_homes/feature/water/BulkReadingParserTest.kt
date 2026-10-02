package com.example.yolo_homes.feature.water

import com.example.yolo_homes.data.model.Flat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BulkReadingParserTest {

    private val flats = listOf(
        Flat(id = "a1", flatNo = "101"),
        Flat(id = "a2", flatNo = "104"),
        Flat(id = "g1", flatNo = "G01")
    )

    @Test
    fun `matches flat and reading separated by a dash`() {
        val result = parseOcrLines("101 - 23456", flats)
        assertEquals("101", result[0].matchedFlat?.flatNo)
        assertEquals(23456.0, result[0].parsedReading)
    }

    @Test
    fun `matches flat case-insensitively`() {
        val result = parseOcrLines("G01: 9055", flats)
        assertEquals("G01", result[0].matchedFlat?.flatNo)
        assertEquals(9055.0, result[0].parsedReading)
    }

    @Test
    fun `ignores a trailing unit token after the reading`() {
        val result = parseOcrLines("104   19928 L", flats)
        assertEquals("104", result[0].matchedFlat?.flatNo)
        assertEquals(19928.0, result[0].parsedReading)
    }

    @Test
    fun `a noise line with no numbers matches nothing`() {
        val result = parseOcrLines("blah blah no numbers here", flats)
        assertNull(result[0].matchedFlat)
        assertNull(result[0].parsedReading)
    }

    @Test
    fun `still extracts the reading even when the flat is not in the building list`() {
        val result = parseOcrLines("502, 12000", flats)
        assertNull(result[0].matchedFlat)
        assertEquals(12000.0, result[0].parsedReading)
    }

    @Test
    fun `one ParsedLine per non-blank input line`() {
        val text = "101 - 23456\n\nG01: 9055\n"
        val result = parseOcrLines(text, flats)
        assertEquals(2, result.size)
    }
}
