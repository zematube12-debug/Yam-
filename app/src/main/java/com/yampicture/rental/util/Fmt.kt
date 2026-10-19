package com.yampicture.rental.util

import com.yampicture.rental.logic.EthiopianCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Date / number formatting that follows the in-app language. */
class Fmt(val lang: String, val ethiopian: Boolean) {
    private val locale = Locale(lang)
    private val dateFormat = SimpleDateFormat("d MMM yyyy", locale)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dayFormat = SimpleDateFormat("EEE d MMM", locale)
    private val monthFormat = SimpleDateFormat("LLLL yyyy", locale)

    fun num(n: Long): String = String.format(Locale.US, "%,d", n)

    fun date(ms: Long): String = dateFormat.format(Date(ms))

    fun time(ms: Long): String = timeFormat.format(Date(ms))

    fun dateTime(ms: Long): String = "${date(ms)}, ${time(ms)}"

    fun day(ms: Long): String = dayFormat.format(Date(ms))

    fun month(year: Int, month0: Int): String {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month0, 1)
        return monthFormat.format(c.time)
    }

    /** Ethiopian calendar date, e.g. "12 መስከረም 2019". */
    fun eth(ms: Long): String {
        val e = EthiopianCalendar.fromMillis(ms)
        val names = if (lang == "am") EthiopianCalendar.monthsAm else EthiopianCalendar.monthsEn
        return "${e.day} ${names[e.month - 1]} ${e.year}"
    }
}

object DateTools {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** Material date pickers speak "UTC midnight of the chosen day". This converts a local moment to that. */
    fun localToUtcDay(ms: Long): Long {
        val l = Calendar.getInstance()
        l.timeInMillis = ms
        val u = Calendar.getInstance(utc)
        u.clear()
        u.set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
        return u.timeInMillis
    }

    /** Combine a picker's UTC day with a local hour and minute. */
    fun combine(utcDay: Long, hour: Int, minute: Int): Long {
        val u = Calendar.getInstance(utc)
        u.timeInMillis = utcDay
        val l = Calendar.getInstance()
        l.clear()
        l.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
        return l.timeInMillis
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance()
        ca.timeInMillis = a
        val cb = Calendar.getInstance()
        cb.timeInMillis = b
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
