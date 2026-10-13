package com.readyplayerai.gainsgoblin

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class CSVFile(private val inputStream: InputStream) {

    fun read(): MutableList<Array<String>> {
        val resultList = mutableListOf<Array<String>>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        try {
            var csvLine = reader.readLine()
            while (csvLine != null) {
                val row = Pattern.compile(",").split(csvLine)
                resultList.add(row)
                csvLine = reader.readLine()
            }
        } catch (ex: IOException) {
            throw RuntimeException("Error in reading CSV file: $ex")
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                throw RuntimeException("Error while closing input stream: $e")
            }
        }
        return resultList
    }
}