package com.yampicture.rental.data

import androidx.room.Embedded
import androidx.room.Relation

/** A rental together with its customer, items and every ledger entry linked to it. */
data class RentalFull(
    @Embedded val rental: Rental,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: Customer,
    @Relation(parentColumn = "id", entityColumn = "rentalId")
    val items: List<RentalItem>,
    @Relation(parentColumn = "id", entityColumn = "rentalId")
    val payments: List<LedgerEntry>,
)
