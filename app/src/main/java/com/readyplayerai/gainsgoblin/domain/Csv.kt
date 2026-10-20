package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.util.ArrayList

/**
 * Converts raw CSV rows into [WorkoutSet] instances.
 *
 * Pure port of the legacy `csvToSets()`. Row 0 (the header) is skipped. Columns are
 * read as `date=0`, `exerciseName=1`, `category=2`, `reps=3`, `weight=4`; a comment
 * is read from column 5 ONLY when the row has exactly six columns, otherwise it is
 * an empty string.
 *
 * QUIRK PRESERVED: the legacy EXPORT writes `Date,Exercise,Category,Weight,Reps,
 * Comment`, but the legacy IMPORT reads column 3 as reps and column 4 as weight.
 * Importing a file produced by this app therefore swaps reps and weight. This port
 * deliberately keeps the swapped import behavior for parity; do not fix it here.
 */
fun setsFromCsvRows(rows: List<Array<String>>): List<WorkoutSet> {
    val sets = ArrayList<WorkoutSet>()

    // Row 0 is only the header.
    for (i in 1 until rows.size) {
        val row = rows[i]
        val date = row[0]
        val exerciseName = row[1]
        val category = row[2]
        val reps = java.lang.Double.parseDouble(row[3])
        val weight = java.lang.Double.parseDouble(row[4])

        var comment = ""
        if (row.size == 6) {
            comment = row[5]
        }

        sets.add(WorkoutSet(date, exerciseName, category, reps, weight, comment))
    }

    return sets
}
