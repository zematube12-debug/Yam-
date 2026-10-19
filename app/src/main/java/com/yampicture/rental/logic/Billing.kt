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

/** Gregorian -> Ethiopian calendar (display only). */
object EthiopianCalendar {
    data class EthDate(val year: Int, val month: Int, val day: Int)

    private fun gregorianToJdn(y: Int, m: Int, d: Int): Long {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

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

    val monthsEn = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit", "Megabit",
        "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume",
    )
    val monthsAm = listOf(
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
        "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን",
    )
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
