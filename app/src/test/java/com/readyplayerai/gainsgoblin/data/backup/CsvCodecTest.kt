package com.readyplayerai.gainsgoblin.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CsvCodecTest {

    @Test
    fun plainRowsAndTrailingNewlineHaveNoExtraRow() {
        val rows = CsvCodec.parse("a,b\nc,d\n")
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b"), rows[0])
        assertEquals(listOf("c", "d"), rows[1])
    }

    @Test
    fun quotedFieldWithComma() {
        val rows = CsvCodec.parse("a,\"b,c\"")
        assertEquals(1, rows.size)
        assertEquals(listOf("a", "b,c"), rows[0])
    }

    @Test
    fun escapedQuote() {
        val rows = CsvCodec.parse("a,\"b\"\"c\"")
        assertEquals(1, rows.size)
        assertEquals(listOf("a", "b\"c"), rows[0])
    }

    @Test
    fun embeddedNewlineInQuotedField() {
        val rows = CsvCodec.parse("a,\"b\nc\",d")
        assertEquals(1, rows.size)
        assertEquals(listOf("a", "b\nc", "d"), rows[0])
    }

    @Test
    fun crlfAndBom() {
        val rows = CsvCodec.parse("\uFEFFa,b\r\nc,d\r\n")
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b"), rows[0])
        assertEquals(listOf("c", "d"), rows[1])
    }

    @Test
    fun unterminatedQuoteThrows() {
        try {
            CsvCodec.parse("a,\"b")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
