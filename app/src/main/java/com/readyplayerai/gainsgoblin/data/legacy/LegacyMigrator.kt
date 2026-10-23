package com.readyplayerai.gainsgoblin.data.legacy

import android.content.Context
import androidx.room.withTransaction
import com.readyplayerai.gainsgoblin.data.backup.BackupFileStore
import com.readyplayerai.gainsgoblin.data.db.GainsGoblinDatabase
import com.readyplayerai.gainsgoblin.data.db.MetaEntity
import com.readyplayerai.gainsgoblin.data.db.WorkoutDao
import com.readyplayerai.gainsgoblin.data.db.toEntity
import com.readyplayerai.gainsgoblin.data.db.toModel
import com.readyplayerai.gainsgoblin.domain.defaultKnownExercises
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MigrationResult {
    data object AlreadyDone : MigrationResult
    data class SeededFreshInstall(val exercises: Int) : MigrationResult
    data class Migrated(val sets: Int, val exercises: Int, val fingerprint: String) : MigrationResult
    data class Failed(val error: String, val rawWorkouts: String?, val rawKnown: String?) : MigrationResult
}

@Singleton
class LegacyMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storeReader: LegacyStoreReader,
    private val database: GainsGoblinDatabase,
    private val backupFileStore: BackupFileStore,
    private val dao: WorkoutDao
) {
    suspend fun migrateIfNeeded(): MigrationResult {
        if (dao.getMeta("legacy_migration")?.value == "done") {
            return MigrationResult.AlreadyDone
        }

        if (dao.countSets() > 0 || dao.countExercises() > 0) {
            dao.putMeta(MetaEntity("legacy_migration", "done"))
            return MigrationResult.AlreadyDone
        }

        val raw = try {
            storeReader.read()
        } catch (e: Exception) {
            return MigrationResult.Failed(e.toString(), null, null)
        }

        if (raw.rawWorkouts.isNullOrBlank() && raw.rawKnown.isNullOrBlank()) {
            val seeded = defaultKnownExercises()
            database.withTransaction {
                dao.upsertExercises(seeded.mapIndexed { i, e -> e.toEntity(i) })
                dao.putMeta(MetaEntity("legacy_migration", "done"))
            }
            return MigrationResult.SeededFreshInstall(seeded.size)
        }

        val snapshot = try {
            decodeLegacySnapshot(raw.rawWorkouts, raw.rawKnown)
        } catch (e: Exception) {
            return MigrationResult.Failed(e.toString(), raw.rawWorkouts, raw.rawKnown)
        }

        try {
            val backupDir = File(context.filesDir, "backups")
            backupDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
            val backupFile = File(backupDir, "legacy-pre-migration-$timestamp.txt")
            backupFile.writeText(
                "WORKOUTS\n" +
                    (raw.rawWorkouts ?: "") +
                    "\n\nKNOWN_EXERCISES\n" +
                    (raw.rawKnown ?: ""),
                Charsets.UTF_8
            )
            backupFileStore.prune(3)
        } catch (e: Exception) {
            return MigrationResult.Failed(e.toString(), raw.rawWorkouts, raw.rawKnown)
        }

        return try {
            val planned = MigrationPlanner.plan(snapshot, now = System.currentTimeMillis())

            database.withTransaction {
                dao.deleteAllSets()
                dao.deleteAllExercises()
                dao.insertSets(planned.sets)
                dao.upsertExercises(planned.exercises)

                val storedSets = dao.getAllSets().map { it.toModel() }
                val storedExercises = dao.getAllExercises().map { it.toModel() }
                val storedFingerprint = fingerprintWorkoutData(storedSets, storedExercises)

                if (storedSets.size != snapshot.setCount ||
                    storedExercises.size != snapshot.exerciseCount ||
                    storedFingerprint != snapshot.fingerprint
                ) {
                    throw IllegalStateException(
                        "Legacy migration verification failed: " +
                            "sets=${storedSets.size}/${snapshot.setCount}, " +
                            "exercises=${storedExercises.size}/${snapshot.exerciseCount}, " +
                            "fingerprint=$storedFingerprint/${snapshot.fingerprint}"
                    )
                }

                dao.putMeta(MetaEntity("legacy_migration", "done"))
                dao.putMeta(MetaEntity("legacy_migration_at", System.currentTimeMillis().toString()))
                dao.putMeta(MetaEntity("legacy_migration_fingerprint", snapshot.fingerprint))
                dao.putMeta(MetaEntity("legacy_migration_set_count", snapshot.setCount.toString()))
                dao.putMeta(MetaEntity("legacy_migration_exercise_count", snapshot.exerciseCount.toString()))
            }

            MigrationResult.Migrated(snapshot.setCount, snapshot.exerciseCount, snapshot.fingerprint)
        } catch (e: Exception) {
            MigrationResult.Failed(e.toString(), raw.rawWorkouts, raw.rawKnown)
        }
    }
}
