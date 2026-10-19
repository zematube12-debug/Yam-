package com.yampicture.rental.logic

import com.yampicture.rental.data.LedgerEntry
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.data.Types

enum class RentalStatus { RESERVED, ACTIVE, OVERDUE, RETURNED }

fun RentalFull.status(now: Long): RentalStatus = when {
    rental.returnedAt != null -> RentalStatus.RETURNED
    rental.startAt > now -> RentalStatus.RESERVED
    now > rental.dueAt -> RentalStatus.OVERDUE
    else -> RentalStatus.ACTIVE
}

/** Price of one day for everything on this rental. */
val RentalFull.dailyTotal: Long get() = items.sumOf { it.dailyRate }

fun RentalFull.extraDays(now: Long, graceMs: Long): Int =
    rental.finalExtraDays ?: Billing.extraDaysAt(rental.dueAt, now, graceMs)

fun RentalFull.billedDays(now: Long, graceMs: Long): Int = rental.days + extraDays(now, graceMs)

fun RentalFull.total(now: Long, graceMs: Long): Long = dailyTotal * billedDays(now, graceMs)

/** Money received from the customer minus anything refunded to them. */
val RentalFull.paid: Long
    get() = payments.sumOf { if (it.type == Types.INCOME) it.amount else -it.amount }

fun RentalFull.balance(now: Long, graceMs: Long): Long = total(now, graceMs) - paid

object Availability {
    /**
     * The rental that blocks [equipmentId] between [start] and [end], or null if it is free.
     * A rental that is overdue and not yet returned blocks the item until it comes back.
     */
    fun conflict(
        rentals: List<RentalFull>,
        equipmentId: Long,
        start: Long,
        end: Long,
        now: Long,
        ignoreRentalId: Long = 0,
    ): RentalFull? = rentals.firstOrNull { f ->
        f.rental.returnedAt == null &&
            f.rental.id != ignoreRentalId &&
            f.items.any { it.equipmentId == equipmentId } &&
            run {
                val theirEnd = if (now > f.rental.dueAt) Long.MAX_VALUE else f.rental.dueAt
                start < theirEnd && f.rental.startAt < end
            }
    }
}

data class EquipmentStat(
    val equipmentId: Long,
    val name: String,
    val nameAm: String,
    val rentals: Int,
    val days: Int,
    val revenue: Long,
)

data class CustomerStat(val name: String, val rentals: Int, val billed: Long)

data class MonthReport(
    val year: Int,
    val month: Int,
    val income: Long,
    val expense: Long,
    val incomeByCategory: List<Pair<String, Long>>,
    val expenseByCategory: List<Pair<String, Long>>,
    val rentalCount: Int,
    val billed: Long,
    val paidOnRentals: Long,
    val outstanding: Long,
    val lateDays: Int,
    val equipment: List<EquipmentStat>,
    val customers: List<CustomerStat>,
) {
    val net: Long get() = income - expense
}

object Reports {
    fun build(
        year: Int,
        month: Int,
        rentals: List<RentalFull>,
        ledger: List<LedgerEntry>,
        now: Long,
        graceMs: Long,
    ): MonthReport {
        val (start, end) = DateUtil.monthBounds(year, month)
        val entries = ledger.filter { it.at in start until end }
        val incomeEntries = entries.filter { it.type == Types.INCOME }
        val expenseEntries = entries.filter { it.type == Types.EXPENSE }

        val inMonth = rentals.filter { it.rental.startAt in start until end }

        val equipmentStats = HashMap<Long, EquipmentStat>()
        for (f in inMonth) {
            val days = f.billedDays(now, graceMs)
            for (item in f.items) {
                val old = equipmentStats[item.equipmentId]
                equipmentStats[item.equipmentId] = EquipmentStat(
                    equipmentId = item.equipmentId,
                    name = item.name,
                    nameAm = item.nameAm,
                    rentals = (old?.rentals ?: 0) + 1,
                    days = (old?.days ?: 0) + days,
                    revenue = (old?.revenue ?: 0L) + item.dailyRate * days,
                )
            }
        }

        val customerStats = inMonth
            .groupBy { it.customer.id }
            .map { (_, list) ->
                CustomerStat(
                    name = list.first().customer.name,
                    rentals = list.size,
                    billed = list.sumOf { it.total(now, graceMs) },
                )
            }
            .sortedByDescending { it.billed }

        return MonthReport(
            year = year,
            month = month,
            income = incomeEntries.sumOf { it.amount },
            expense = expenseEntries.sumOf { it.amount },
            incomeByCategory = incomeEntries.groupBy { it.category }
                .map { (k, v) -> k to v.sumOf { it.amount } }
                .sortedByDescending { it.second },
            expenseByCategory = expenseEntries.groupBy { it.category }
                .map { (k, v) -> k to v.sumOf { it.amount } }
                .sortedByDescending { it.second },
            rentalCount = inMonth.size,
            billed = inMonth.sumOf { it.total(now, graceMs) },
            paidOnRentals = inMonth.sumOf { it.paid },
            outstanding = inMonth.sumOf { maxOf(0L, it.balance(now, graceMs)) },
            lateDays = inMonth.sumOf { it.extraDays(now, graceMs) },
            equipment = equipmentStats.values.sortedByDescending { it.revenue },
            customers = customerStats,
        )
    }
}
