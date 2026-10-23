package com.readyplayerai.gainsgoblin.data.backup

object CsvCodec {

    fun parse(text: String): List<List<String>> {
        val input = if (text.startsWith('\uFEFF')) text.substring(1) else text
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var sawAny = false
        var line = 1
        var i = 0

        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                when (c) {
                    '"' -> {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            field.append('"')
                            i += 2
                        } else {
                            inQuotes = false
                            i++
                        }
                    }
                    '\n' -> {
                        line++
                        field.append(c)
                        i++
                    }
                    else -> {
                        field.append(c)
                        i++
                    }
                }
            } else {
                when (c) {
                    '"' -> {
                        if (field.isEmpty()) {
                            inQuotes = true
                        } else {
                            field.append('"')
                        }
                        sawAny = true
                        i++
                    }
                    ',' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        sawAny = true
                        i++
                    }
                    '\r' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                        sawAny = false
                        if (i + 1 < input.length && input[i + 1] == '\n') i += 2 else i++
                        line++
                    }
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                        sawAny = false
                        i++
                        line++
                    }
                    else -> {
                        field.append(c)
                        sawAny = true
                        i++
                    }
                }
            }
        }

        if (inQuotes) {
            throw IllegalArgumentException("Unterminated quoted field at line $line")
        }

        if (sawAny) {
            row.add(field.toString())
            rows.add(row)
        }

        return rows
    }
}
