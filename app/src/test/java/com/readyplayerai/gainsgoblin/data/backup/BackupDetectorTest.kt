package com.readyplayerai.gainsgoblin.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDetectorTest {

    private fun detect(text: String) = BackupDetector.detect(text.toByteArray(Charsets.UTF_8))

    @Test
    fun detectsJson() {
        assertEquals(
            BackupFormat.GAINS_GOBLIN_JSON,
            detect("""{"schemaVersion":2,"exportedAt":"2026-01-01T00:00:00Z","exercises":[],"sets":[]}""")
        )
    }

    @Test
    fun detectsLegacyCsv() {
        assertEquals(
            BackupFormat.GAINS_GOBLIN_CSV,
            detect("Date,Exercise,Category,Weight (kg),Reps,Comment\n2026-01-05,Bench,Chest,8,60,")
        )
    }

    @Test
    fun detectsFitNotesCsv() {
        assertEquals(
            BackupFormat.FITNOTES_CSV,
            detect("Date,Exercise,Category,Weight (kgs),Reps,Distance,Distance Unit,Time\n")
        )
    }

    @Test
    fun detectsWithLeadingWhitespaceAndBom() {
        assertEquals(
            BackupFormat.GAINS_GOBLIN_JSON,
            detect("\uFEFF   \n  {\"schemaVersion\":2}")
        )
        assertEquals(
            BackupFormat.FITNOTES_CSV,
            detect("\uFEFF  \nDate,Exercise,Category,Weight (kgs),Reps")
        )
    }
}
