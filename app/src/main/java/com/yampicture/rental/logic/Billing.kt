package com.yampicture.rental.logic

import java.util.Calendar

/**
 * The studio's late-return rule, in one place.
 *
 * A rental is billed in 24h days. When the customer keeps the equipment past the due time:
 *  - up to [graceMs] (default half a day) of lateness is free,
 *  - once the lateness is MORE than the grace period, one extra day is billed,
 *  - after that, one more extra day is billed every further 24 hours.
 * Before each extra day is added, the customer/owner is warned with a notification.
 */
object Billing {
    const val HOUR_MS = 3_600_000L
    const val DAY_MS = 24L * HOUR_MS

    fun extraDaysAt(dueAt: Long, at: Long, graceMs: Long): Int {
        val over = at - dueAt
        if (over <= graceMs) return 0
        return ((over - graceMs) / DAY_MS).toInt() + 1
    }

    /** The moment extra day number [k] (1-based) gets added. */
    fun extraDayThreshold(dueAt: Long, graceMs: Long, k: Int): Long =
        dueAt + graceMs + (k - 1).toLong() * DAY_MS
}

data class AlertState(
    val dueNotified: Boolean,
    val warnedIndex: Int,
    val addedIndex: Int,
)

data class AlertDecision(
    val newState: AlertState,
    val notifyDue: Boolean,
    /** Extra-day number we are warning about (0 = no warning now). */
    val warnIndex: Int,
    /** Total extra days we are announcing as just added (0 = nothing new). */
    val addedIndex: Int,
)

object RentalAlerts {
    /** Decides which notifications are due for a rental that is still out. Each fires once. */
    fun evaluate(dueAt: Long, now: Long, graceMs: Long, leadMs: Long, state: AlertState): AlertDecision {
        if (now < dueAt) return AlertDecision(state, false, 0, 0)

        var dueNotified = state.dueNotified
        var warned = state.warnedIndex
        var addedIdx = state.addedIndex
        var notifyDue = false
        var warn = 0
        var added = 0

        if (!dueNotified) {
            notifyDue = true
            dueNotified = true
        }

        val extra = Billing.extraDaysAt(dueAt, now, graceMs)
        if (extra > addedIdx) {
            added = extra
            addedIdx = extra
            if (warned < extra) warned = extra
        }

        val next = extra + 1
        val threshold = Billing.extraDayThreshold(dueAt, graceMs, next)
        if (warned < next && now >= threshold - leadMs) {
            warn = next
            warned = next
        }

        return AlertDecision(AlertState(dueNotified, warned, addedIdx), notifyDue, warn, added)
    }
}

/** Gregorian <-> Ethiopian calendar and the Ethiopian 12-hour clock. */
object EthiopianCalendar {
    data class EthDate(val year: Int, val month: Int, val day: Int)

    private fun gregorianToJdn(y: Int, m: Int, d: Int): Long {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun jdnToGregorian(jdn: Long): Triple<Int, Int, Int> {
        val f = jdn + 1401 + (((4 * jdn + 274277) / 146097) * 3) / 4 - 38
        val e = 4 * f + 3
        val g = (e % 1461) / 4
        val h = 5 * g + 2
        val day = ((h % 153) / 5 + 1).toInt()
        val month = (((h / 153 + 2) % 12) + 1).toInt()
        val year = (e / 1461 - 4716 + (12 + 2 - month) / 12).toInt()
        return Triple(year, month, day)
    }

    private fun ethiopianToJdn(year: Int, month: Int, day: Int): Long =
        1723856L + 365L + 365L * (year - 1) + year / 4 + 30L * month + day - 31

    /** [m] is 1-12. */
    fun fromGregorian(y: Int, m: Int, d: Int): EthDate {
        val jdn = gregorianToJdn(y, m, d)
        val r = (jdn - 1723856L) % 1461L
        val n = r % 365L + 365L * (r / 1460L)
        val year = 4L * ((jdn - 1723856L) / 1461L) + r / 365L - r / 1460L
        return EthDate(year.toInt(), (n / 30L + 1L).toInt(), (n % 30L + 1L).toInt())
    }

    fun fromMillis(ms: Long): EthDate {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** Ethiopian [month] is 1-13. Returns Gregorian (year, month 1-12, day). */
    fun toGregorian(year: Int, month: Int, day: Int): Triple<Int, Int, Int> =
        jdnToGregorian(ethiopianToJdn(year, month, day))

    fun isLeapYear(year: Int): Boolean = year % 4 == 3

    fun daysInMonth(year: Int, month: Int): Int = when {
        month < 13 -> 30
        isLeapYear(year) -> 6
        else -> 5
    }

    /** 0 = Monday ... 6 = Sunday, for the given Ethiopian date. */
    fun weekdayIndex(year: Int, month: Int, day: Int): Int =
        (ethiopianToJdn(year, month, day) % 7L).toInt()

    val monthsEn = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit", "Megabit",
        "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume",
    )
    val monthsAm = listOf(
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
        "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን",
    )

    /** Monday first. */
    val weekdaysAm = listOf("ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "አርብ", "ቅዳሜ", "እሑድ")
    val weekdaysEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weekdaysShortAm = listOf("ሰ", "ማ", "ረ", "ሐ", "አ", "ቅ", "እ")
    val weekdaysShortEn = listOf("M", "T", "W", "T", "F", "S", "S")

    // ------------------------------------------------------------ Ethiopian clock
    // 6:00 on a normal clock is 12:00 on the Ethiopian clock, so the hour is shifted by six.
    // Parts of the day: 0 = morning (6-12), 1 = day (12-18), 2 = evening (18-24), 3 = night (0-6).

    fun ethHour(hour24: Int): Int {
        val x = ((hour24 - 6) % 12 + 12) % 12
        return if (x == 0) 12 else x
    }

    fun periodOf(hour24: Int): Int = when (hour24) {
        in 6..11 -> 0
        in 12..17 -> 1
        in 18..23 -> 2
        else -> 3
    }

    /** The first 24h hour of each part of the day. */
    val periodStart = listOf(6, 12, 18, 0)

    val periodsAm = listOf("ጠዋት", "ቀን", "ማታ", "ሌሊት")
    val periodsEn = listOf("morning", "afternoon", "evening", "night")

    fun formatTime(hour24: Int, minute: Int, amharic: Boolean): String {
        val names = if (amharic) periodsAm else periodsEn
        return "${ethHour(hour24)}:${minute.toString().padStart(2, '0')} ${names[periodOf(hour24)]}"
    }
}

object DateUtil {
    /** Start (inclusive) and end (exclusive) of a month. [month] is 0-11. */
    fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, 1, 0, 0, 0)
        val start = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        return start to c.timeInMillis
    }

    fun endOfDay(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    fun startOfDay(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** year * 12 + month(0-11), handy as one saveable number for month pickers. */
    fun monthIndexOf(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
    }
}
