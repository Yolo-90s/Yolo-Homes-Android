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
    fun `extracts both current and previous from the real notebook's column order`() {
        // M.No, Flat.No, Total Reading, Old Reading, New
        val result = parseOcrLines("01973 101 43621 33362 10259", flats)
        assertEquals("101", result[0].matchedFlat?.flatNo)
        assertEquals(43621.0, result[0].parsedCurrent)
        assertEquals(33362.0, result[0].parsedPrevious)
    }

    @Test
    fun `matches flat case-insensitively`() {
        val result = parseOcrLines("G01: 9055 8000", flats)
        assertEquals("G01", result[0].matchedFlat?.flatNo)
        assertEquals(9055.0, result[0].parsedCurrent)
        assertEquals(8000.0, result[0].parsedPrevious)
    }

    @Test
    fun `a simple flat-plus-one-number line leaves previous null`() {
        val result = parseOcrLines("101 - 23456", flats)
        assertEquals(23456.0, result[0].parsedCurrent)
        assertNull(result[0].parsedPrevious)
    }

    @Test
    fun `a noise line with no numbers matches nothing`() {
        val result = parseOcrLines("blah blah no numbers here", flats)
        assertNull(result[0].matchedFlat)
        assertNull(result[0].parsedCurrent)
    }

    @Test
    fun `one ParsedLine per non-blank input line`() {
        val text = "101 - 23456\n\nG01: 9055\n"
        val result = parseOcrLines(text, flats)
        assertEquals(2, result.size)
    }

    @Test
    fun `csv with a recognizable header uses the named columns`() {
        val csv = "M.No,Flat No,Total Reading,Old Reading,New\n01973,101,43621,33362,10259"
        val result = parseCsvRows(csv, flats)
        assertEquals(1, result.size)
        assertEquals("101", result[0].matchedFlat?.flatNo)
        assertEquals(43621.0, result[0].parsedCurrent)
        assertEquals(33362.0, result[0].parsedPrevious)
    }

    @Test
    fun `csv with no header falls back to the notebook's own column order`() {
        val csv = "01973,101,43621,33362,10259\n05721,201,56885,46021,10864"
        val result = parseCsvRows(csv, flats)
        assertEquals(2, result.size)
        assertEquals("101", result[0].matchedFlat?.flatNo)
        assertEquals(43621.0, result[0].parsedCurrent)
        assertEquals(33362.0, result[0].parsedPrevious)
    }
}
