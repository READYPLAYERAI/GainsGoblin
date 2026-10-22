package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.util.ArrayList

/**
 * Converts raw CSV rows into [WorkoutSet] instances.
 *
 * Row 0 (the header) is skipped. Columns are read as `date=0`, `exerciseName=1`,
 * `category=2`, `weight=3`, `reps=4`, matching the export layout
 * `Date,Exercise,Category,Weight (kg),Reps,Comment`; a comment is read from column 5
 * ONLY when the row has exactly six columns, otherwise it is an empty string.
 */
fun setsFromCsvRows(rows: List<Array<String>>): List<WorkoutSet> {
    val sets = ArrayList<WorkoutSet>()

    // Row 0 is only the header.
    for (i in 1 until rows.size) {
        val row = rows[i]
        val date = row[0]
        val exerciseName = row[1]
        val category = row[2]
        val weight = java.lang.Double.parseDouble(row[3])
        val reps = java.lang.Double.parseDouble(row[4])

        var comment = ""
        if (row.size == 6) {
            comment = row[5]
        }

        sets.add(WorkoutSet(date, exerciseName, category, reps, weight, comment))
    }

    return sets
}
