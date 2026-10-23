package com.readyplayerai.gainsgoblin.data.backup

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupFileStoreTest {

    private lateinit var root: File
    private lateinit var store: BackupFileStore

    @Before
    fun setUp() {
        root = Files.createTempDirectory("gainsgoblin-backups").toFile()
        store = BackupFileStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun writePreImportCreatesFileWithExactTextAndTimestamp() {
        val now = 1_700_000_000_000L
        val file = store.writePreImport("{\"hello\":1}", "before-import", now)

        assertTrue(file.name.startsWith("pre-import-before-import-"))
        assertTrue(file.name.endsWith(".json"))
        assertEquals("{\"hello\":1}", file.readText(Charsets.UTF_8))
        assertEquals(now, file.lastModified())
    }

    @Test
    fun pruneKeepsThreeNewestPreImportFiles() {
        val files = (1..5).map { i ->
            store.writePreImport("pre-$i", "before-import", 1_000L + i)
        }

        store.prune(3)

        val remaining = store.listPreImports()
        assertEquals(3, remaining.size)
        assertEquals(setOf(files[2].name, files[3].name, files[4].name), remaining.map { it.name }.toSet())
        assertFalse(files[0].exists())
        assertFalse(files[1].exists())
    }

    @Test
    fun pruneKeepsThreeNewestLegacyFilesIndependently() {
        val legacy = (1..5).map { i ->
            File(root, "legacy-pre-migration-$i.json").apply {
                writeText("legacy-$i", Charsets.UTF_8)
                setLastModified(2_000L + i)
            }
        }
        val preImport = (1..2).map { i ->
            store.writePreImport("pre-$i", "before-import", 3_000L + i)
        }

        store.prune(3)

        val legacyRemaining = root.listFiles()!!
            .filter { it.name.startsWith("legacy-pre-migration-") }
            .map { it.name }
            .toSet()
        assertEquals(setOf(legacy[2].name, legacy[3].name, legacy[4].name), legacyRemaining)
        assertFalse(legacy[0].exists())
        assertFalse(legacy[1].exists())
        assertTrue(preImport.all { it.exists() })
    }

    @Test
    fun latestPreImportReturnsNewestFile() {
        store.writePreImport("old", "before-import", 1_000L)
        val middle = store.writePreImport("middle", "before-import", 2_000L)
        val newest = store.writePreImport("new", "before-import", 3_000L)

        assertEquals(newest.name, store.latestPreImport()?.name)
        assertTrue(middle.exists())
    }

    @Test
    fun readBackupRejectsUnsafeNames() {
        assertNull(store.readBackup("../escape.txt"))
        assertNull(store.readBackup("a/b.txt"))
        assertNull(store.readBackup("a\\b.txt"))
    }

    @Test
    fun deleteBackupDeletesExistingFile() {
        val file = File(root, "pre-import-before-import-20200101-000000-000.json").apply {
            writeText("data", Charsets.UTF_8)
        }

        assertTrue(store.deleteBackup(file.name))
        assertFalse(file.exists())
    }
}
