package com.readyplayerai.gainsgoblin.ui.compose.progress

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formats a weight in kilograms, rounded to at most one decimal and dropping a
 * trailing `.0`: `60.0 -> "60"`, `107.5 -> "107.5"`, `146.66666666666666 -> "146.7"`.
 */
fun formatWeight(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

/**
 * Formats a volume as a grouped integer using a normal space as the thousands
 * separator: `40610 -> "40 610"`, `945 -> "945"`.
 */
fun formatVolume(value: Double): String {
    return String.format(Locale.US, "%,d", value.roundToInt()).replace(',', ' ')
}

/**
 * Formats a volume compactly: values of 1000 kg or more
 * become tonnes with one decimal (`40610.0 -> "40.6 t"`), otherwise the grouped
 * kilogram value is returned (`945.0 -> "945 kg"`).
 */
fun formatTonsOrKg(value: Double): String {
    return if (value >= 1000.0) {
        String.format(Locale.US, "%.1f t", value / 1000.0)
    } else {
        formatVolume(value) + " kg"
    }
}

/**
 * Formats a `yyyy-MM-dd` date as a short day and month (`"2026-10-23" -> "23 Oct"`).
 * Null or blank input returns an empty string; unparsable input is returned raw.
 */
fun shortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parsed = parseIsoDate(iso) ?: return iso
    return SimpleDateFormat("d MMM", Locale.US).format(parsed)
}

/**
 * Formats a `yyyy-MM-dd` date as its month and year
 * (`"2026-10-23" -> "October 2026"`). Null or blank input returns an empty
 * string; unparsable input is returned raw.
 */
fun monthLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parsed = parseIsoDate(iso) ?: return iso
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
}

/**
 * Formats a `yyyy-MM-dd` date as a short weekday (`"2026-10-23" -> "Fri"`).
 * Null or blank input returns an empty string; unparsable input is returned raw.
 */
fun weekdayShort(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parsed = parseIsoDate(iso) ?: return iso
    return SimpleDateFormat("EEE", Locale.US).format(parsed)
}

/**
 * Formats a `yyyy-MM-dd` date with its year (`"2026-10-23" -> "23 Oct 2026"`).
 * Null or blank input returns an empty string; unparsable input is returned raw.
 */
fun dayWithYear(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parsed = parseIsoDate(iso) ?: return iso
    return SimpleDateFormat("d MMM yyyy", Locale.US).format(parsed)
}

/** Parses a `yyyy-MM-dd` date, returning null when the input is blank or unparsable. */
private fun parseIsoDate(iso: String?): Date? {
    if (iso.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
    } catch (e: ParseException) {
        null
    }
}
