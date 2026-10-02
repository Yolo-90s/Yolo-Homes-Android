package com.example.yolo_homes.feature.water

import com.example.yolo_homes.data.model.Flat

/** One OCR'd line from a photographed reading list, parsed into a flat + reading guess. */
data class ParsedLine(
    val rawLine: String,
    val matchedFlat: Flat?,
    val parsedReading: Double?
)

private val SEPARATOR_RE = Regex("[\\s\\-:,/]+")
private val NUMERIC_RE = Regex("^[0-9][0-9.]*$")

/**
 * Parses OCR'd text from a photographed meter-reading list into flat+reading pairs. One
 * non-blank line in → one [ParsedLine] out (even unmatched ones), so every OCR line is visible
 * and fixable in the review table rather than silently dropped.
 */
fun parseOcrLines(text: String, flats: List<Flat>): List<ParsedLine> {
    val byFlatNo = flats.associateBy { it.flatNo.lowercase() }

    return text.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { rawLine ->
            val tokens = rawLine.split(SEPARATOR_RE).filter { it.isNotEmpty() }

            var matchedFlat: Flat? = null
            var flatTokenIndex = -1
            tokens.forEachIndexed { i, t ->
                if (matchedFlat == null) {
                    byFlatNo[t.lowercase()]?.let {
                        matchedFlat = it
                        flatTokenIndex = i
                    }
                }
            }

            var parsedReading: Double? = null
            for (i in tokens.indices.reversed()) {
                if (i == flatTokenIndex) continue
                val cleaned = tokens[i].replace(",", "")
                if (NUMERIC_RE.matches(cleaned)) {
                    parsedReading = cleaned.toDoubleOrNull()
                    break
                }
            }

            ParsedLine(rawLine, matchedFlat, parsedReading)
        }
}
