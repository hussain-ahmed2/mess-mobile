package com.messmate.bd.ui.join

/**
 * Extracts the spreadsheet id out of everything a member may paste or scan:
 *
 * * a raw id – `1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms`
 * * a deep link – `messmate://join/1BxiMVs0…`
 * * a sheet link – `https://docs.google.com/spreadsheets/d/1BxiMVs0…/edit?usp=sharing`
 */
object SpreadsheetIdExtractor {

    /**
     * A Google file id is 44 characters long and mixes letters, digits, `-` and
     * `_`. The pattern is strict on purpose – a loose one would also match
     * parts of a `content://` uri and produce a wrong id.
     */
    private val ID_PATTERN = Regex("(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9_-]{28,64}")

    /** Returns the spreadsheet id or `null` when nothing usable was found. */
    fun extract(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        // A full Google Sheet link – take the segment behind "/d/".
        val afterD = text.substringAfter("/d/", missingDelimiterValue = "")
        if (afterD.isNotEmpty()) {
            val id = afterD.substringBefore('/').substringBefore('?')
            if (id.isNotBlank()) return id
        }

        // A deep link of the app.
        val afterJoin = text.substringAfter("/join/", missingDelimiterValue = "")
        if (afterJoin.isNotEmpty()) {
            val id = afterJoin.substringBefore('?').substringBefore('/')
            if (id.isNotBlank()) return id
        }

        // A raw id.
        return ID_PATTERN.find(text)?.value
    }
}
