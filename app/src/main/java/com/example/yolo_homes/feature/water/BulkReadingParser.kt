package com.example.yolo_homes.feature.water

import com.example.yolo_homes.data.model.Flat

/**
 * One parsed line/row from a photographed or CSV-uploaded meter-reading list. The building's
 * real notebook format is: M.No | Flat.No | Total Reading | Old Reading | New.
 */
data class ParsedLine(
    val rawLine: String,
    val matchedFlat: Flat?,
    val parsedCurrent: Double?,
    val parsedPrevious: Double?
)

private val SEPARATOR_RE = Regex("[\\s\\-:,/]+")
private val NUMERIC_RE = Regex("^[0-9][0-9.]*$")

/**
 * Parses OCR'd (or typed/pasted) text from a photographed meter-reading list into flat+reading
 * pairs. One non-blank line in → one [ParsedLine] out (even unmatched ones), so every line is
 * visible and fixable in the review table rather than silently dropped.
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

            // Numbers strictly AFTER the flat token, in order: the real notebook's column order
            // is Flat.No, Total Reading, Old Reading, New — so the first is "current", the
            // second is "previous" (a lone-number line, e.g. a simpler list with no Old Reading
            // column, leaves previous null so the caller falls back to the last known reading).
            val numbersAfterFlat = mutableListOf<Double>()
            for (i in (flatTokenIndex + 1) until tokens.size) {
                val cleaned = tokens[i].replace(",", "")
                if (NUMERIC_RE.matches(cleaned)) {
                    cleaned.toDoubleOrNull()?.let { numbersAfterFlat.add(it) }
                }
            }

            ParsedLine(
                rawLine = rawLine,
                matchedFlat = matchedFlat,
                parsedCurrent = numbersAfterFlat.getOrNull(0),
                parsedPrevious = numbersAfterFlat.getOrNull(1)
            )
        }
}

private val CSV_FLAT_HINTS = listOf("flat")
private val CSV_CURRENT_HINTS = listOf("total", "current", "new reading")
private val CSV_PREVIOUS_HINTS = listOf("old", "previous", "last")

private data class CsvColumns(val flatIdx: Int, val currentIdx: Int, val previousIdx: Int)

private fun splitRow(line: String): List<String> = line.split(Regex("\t|,")).map { it.trim() }

private fun detectColumns(headerCells: List<String>): CsvColumns? {
    val lower = headerCells.map { it.lowercase() }
    fun find(hints: List<String>): Int = lower.indexOfFirst { cell -> hints.any { cell.contains(it) } }
    val flatIdx = find(CSV_FLAT_HINTS)
    val currentIdx = find(CSV_CURRENT_HINTS)
    val previousIdx = find(CSV_PREVIOUS_HINTS)
    if (flatIdx == -1 || currentIdx == -1) return null
    return CsvColumns(flatIdx, currentIdx, previousIdx)
}

/**
 * Parses a CSV (or tab-separated) file's rows. Tries to find columns by header name first
 * ("Flat.No" / "Total Reading" / "Old Reading", in any order/wording that mentions those
 * words); falls back to the exact notebook column order (M.No, Flat.No, Total, Old, New) if no
 * recognizable header row is found.
 */
fun parseCsvRows(text: String, flats: List<Flat>): List<ParsedLine> {
    val byFlatNo = flats.associateBy { it.flatNo.lowercase() }
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return emptyList()

    var dataLines = lines
    var columns = CsvColumns(flatIdx = 1, currentIdx = 2, previousIdx = 3) // notebook's own order
    detectColumns(splitRow(lines[0]))?.let {
        columns = it
        dataLines = lines.drop(1)
    }

    return dataLines.map { rawLine ->
        val cells = splitRow(rawLine)
        val flatCell = cells.getOrNull(columns.flatIdx)?.lowercase() ?: ""
        val matchedFlat = byFlatNo[flatCell]
        fun parseCell(idx: Int): Double? {
            if (idx < 0) return null
            val cleaned = cells.getOrNull(idx)?.replace(",", "")?.trim() ?: return null
            return if (NUMERIC_RE.matches(cleaned)) cleaned.toDoubleOrNull() else null
        }
        ParsedLine(
            rawLine = rawLine,
            matchedFlat = matchedFlat,
            parsedCurrent = parseCell(columns.currentIdx),
            parsedPrevious = parseCell(columns.previousIdx)
        )
    }
}
