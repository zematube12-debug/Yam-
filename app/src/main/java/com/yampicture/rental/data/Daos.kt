package com.yampicture.rental.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment ORDER BY active DESC, sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Equipment>>

    @Insert
    suspend fun insert(e: Equipment): Long

    @Update
    suspend fun update(e: Equipment)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Customer>>

    @Insert
    suspend fun insert(c: Customer): Long

    @Update
    suspend fun update(c: Customer)

    @Query("SELECT * FROM customers WHERE phone = :phone AND phone != '' LIMIT 1")
    suspend fun findByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Customer?
}

@Dao
interface RentalDao {
    @Transaction
    @Query("SELECT * FROM rentals ORDER BY startAt DESC")
    fun observeAll(): Flow<List<RentalFull>>

    @Transaction
    @Query("SELECT * FROM rentals WHERE returnedAt IS NULL")
    suspend fun getActive(): List<RentalFull>

    @Transaction
    @Query("SELECT * FROM rentals")
    suspend fun getAll(): List<RentalFull>

    @Query("UPDATE rentals SET dueNotified = :due, warnedIndex = :warned, addedNotifiedIndex = :added WHERE id = :id")
    suspend fun setAlertState(id: Long, due: Boolean, warned: Int, added: Int)

    @Transaction
    @Query("SELECT * FROM rentals WHERE id = :id")
    suspend fun getFull(id: Long): RentalFull?

    @Insert
    suspend fun insert(r: Rental): Long

    @Insert
    suspend fun insertItems(items: List<RentalItem>)

    @Update
    suspend fun update(r: Rental)

    @Query("DELETE FROM rentals WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger ORDER BY at DESC, id DESC")
    fun observeAll(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger")
    suspend fun getAll(): List<LedgerEntry>

    @Insert
    suspend fun insert(e: LedgerEntry): Long

    @Update
    suspend fun update(e: LedgerEntry)

    @Query("DELETE FROM ledger WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM ledger WHERE rentalId = :rentalId")
    suspend fun deleteForRental(rentalId: Long)
}
