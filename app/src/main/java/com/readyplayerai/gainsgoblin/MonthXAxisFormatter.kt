package com.readyplayerai.gainsgoblin

import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.HashSet

class MonthXAxisFormatter(private val months: List<String>) : ValueFormatter() {
    private val uniqueMonths = HashSet<String>()

    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        if (index < 0 || index >= months.size) {
            return ""
        }
        val month = months[index]
        uniqueMonths.add(month)
        return month
    }
}