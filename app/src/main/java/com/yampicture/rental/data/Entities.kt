package com.yampicture.rental.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A physical rentable item (every unit is its own row, e.g. "Amaran 1" and "Amaran 2"). */
@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameAm: String = "",
    val dailyRate: Long,
    val active: Boolean = true,
    val sortOrder: Int = 0,
) {
    fun label(amharic: Boolean): String = if (amharic && nameAm.isNotBlank()) nameAm else name
}

@Entity(tableName = "customers", indices = [Index("phone")])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
)

@Entity(
    tableName = "rentals",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("customerId")],
)
data class Rental(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val startAt: Long,
    /** Planned number of 24h days. */
    val days: Int,
    val dueAt: Long,
    val returnedAt: Long? = null,
    /** Extra (late) days billed. Null while the rental is still out: it is then computed live. */
    val finalExtraDays: Int? = null,
    val notes: String = "",
    val returnNotes: String = "",
    // --- notification bookkeeping (so every alert is sent exactly once) ---
    val dueNotified: Boolean = false,
    val warnedIndex: Int = 0,
    val addedNotifiedIndex: Int = 0,
)

/** Equipment on a rental. Name and price are copied so later price edits never change old rentals. */
@Entity(
    tableName = "rental_items",
    foreignKeys = [
        ForeignKey(
            entity = Rental::class,
            parentColumns = ["id"],
            childColumns = ["rentalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rentalId"), Index("equipmentId")],
)
data class RentalItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rentalId: Long,
    val equipmentId: Long,
    val name: String,
    val nameAm: String = "",
    val dailyRate: Long,
) {
    fun label(amharic: Boolean): String = if (amharic && nameAm.isNotBlank()) nameAm else name
}

/** One line of the income / expense ledger. Rental deposits and payments land here automatically. */
@Entity(tableName = "ledger", indices = [Index("rentalId"), Index("at")])
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val category: String,
    val amount: Long,
    val at: Long,
    val note: String = "",
    val rentalId: Long? = null,
)

object Types {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

object Cat {
    const val DEPOSIT = "deposit"
    const val PAYMENT = "payment"
    const val SHOOT = "shoot"
    const val OTHER_INCOME = "other_income"

    const val REPAIR = "repair"
    const val PURCHASE = "purchase"
    const val TRANSPORT = "transport"
    const val SALARY = "salary"
    const val BILLS = "bills"
    const val MARKETING = "marketing"
    const val REFUND = "refund"
    const val OTHER_EXPENSE = "other_expense"

    val income = listOf(DEPOSIT, PAYMENT, SHOOT, OTHER_INCOME)
    val expense = listOf(REPAIR, PURCHASE, TRANSPORT, SALARY, BILLS, MARKETING, REFUND, OTHER_EXPENSE)
}
