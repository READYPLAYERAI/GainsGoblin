package com.readyplayerai.gainsgoblin.data.backup

object FitNotesCsv {

    fun decode(text: String): ParsedImport {
        val rows = CsvCodec.parse(text).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return ParsedImport(emptyList(), emptyList())

        val dataRows = if (rows.first().getOrElse(0) { "" }.equals("Date", ignoreCase = true)) {
            rows.drop(1)
        } else {
            rows
        }

        val sets = mutableListOf<ImportedSet>()
        dataRows.forEachIndexed { index, row ->
            val line = index + 2
            require(row.size >= 5) { "Row $line: expected at least 5 columns" }
            val date = normalizeDate(row[0])
            val exerciseName = row[1].trim()
            require(exerciseName.isNotBlank()) { "Row $line: exercise name must not be blank" }
            val category = row[2].trim()
            val weight = row[3].trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("Row $line: invalid weight '${row[3]}'")
            val reps = row[4].trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("Row $line: invalid reps '${row[4]}'")
            sets.add(ImportedSet(date, exerciseName, category, reps, weight, null))
        }

        return ParsedImport(sets, emptyList())
    }
}
