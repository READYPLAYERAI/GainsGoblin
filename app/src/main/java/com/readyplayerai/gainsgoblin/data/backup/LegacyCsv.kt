package com.readyplayerai.gainsgoblin.data.backup

object LegacyCsv {

    fun decode(text: String): ParsedImport {
        val rows = CsvCodec.parse(text).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return ParsedImport(emptyList(), emptyList())

        val firstRow = rows.first()
        val firstCell = firstRow.getOrElse(0) { "" }
        val dataRows: List<List<String>> = when {
            firstCell.equals("Date", ignoreCase = true) -> rows.drop(1)
            firstCell.contains("Exercise", ignoreCase = true) -> rows.drop(1)
            else -> {
                val isDate = runCatching { normalizeDate(firstCell) }.isSuccess
                if (!isDate) {
                    throw IllegalArgumentException("Unrecognized CSV header: $firstRow")
                }
                rows
            }
        }

        val sets = mutableListOf<ImportedSet>()
        var extraColumnRows = 0

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
            val comment = if (row.size > 5) row.drop(5).joinToString(",") else ""
            if (row.size > 6) extraColumnRows++
            sets.add(ImportedSet(date, exerciseName, category, reps, weight, comment))
        }

        val warnings = mutableListOf<String>()
        if (extraColumnRows > 0) {
            warnings.add("Joined extra columns into the comment on $extraColumnRows row(s)")
        }

        return ParsedImport(sets, emptyList(), warnings)
    }
}
