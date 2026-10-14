package com.readyplayerai.gainsgoblin

import org.junit.Test
import org.junit.Assert.*

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.IOException

class CSVFileTest {

    @Test
    fun read_parsesCsvData() {
        val csvFile = CSVFile(
            ByteArrayInputStream("Date,Exercise\n1,Bench\n2026-01-05,Squat,Legs".toByteArray())
        )
        val result = csvFile.read()
        assertEquals(3, result.size)

        assertArrayEquals(arrayOf("Date", "Exercise"), result[0])
        assertArrayEquals(arrayOf("1", "Bench"), result[1])
        assertArrayEquals(arrayOf("2026-01-05", "Squat", "Legs"), result[2])
    }

    @Test
    fun read_ioError_throwsRuntimeException() {
        val evil: InputStream = object : InputStream() {
            override fun read(): Int {
                throw IOException("test error")
            }
        }
        val csvFile = CSVFile(evil)
        try {
            csvFile.read()
            fail("Expected RuntimeException")
        } catch (e: RuntimeException) {
            assertTrue("Message should start with 'Error in reading CSV file'",
                       e.message!!.startsWith("Error in reading CSV file"))
        }
    }

    @Test
    fun read_javaSplitSemantics_dropsTrailingEmptyCells() {
        val csvFile = CSVFile(
            ByteArrayInputStream(("x,y,,\n" +
                                  "2018-10-29,Seated Dumbbell Press,Shoulders,18.0,10,,,\n" +
                                  "a,b,c,d,e,comment").toByteArray())
        )
        val result = csvFile.read()
        assertEquals(3, result.size)

        assertArrayEquals(arrayOf("x", "y"), result[0])
        assertArrayEquals(arrayOf("2018-10-29", "Seated Dumbbell Press", "Shoulders", "18.0", "10"), result[1])
        assertArrayEquals(arrayOf("a", "b", "c", "d", "e", "comment"), result[2])
    }
}