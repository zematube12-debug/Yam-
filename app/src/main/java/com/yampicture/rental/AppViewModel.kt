package com.yampicture.rental

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.yampicture.rental.data.AppDatabase
import com.yampicture.rental.data.Cat
import com.yampicture.rental.data.Customer
import com.yampicture.rental.data.Equipment
import com.yampicture.rental.data.LedgerEntry
import com.yampicture.rental.data.Rental
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.data.RentalItem
import com.yampicture.rental.data.Types
import com.yampicture.rental.logic.Availability
import com.yampicture.rental.logic.Billing
import com.yampicture.rental.logic.dailyTotal
import com.yampicture.rental.logic.extraDays
import com.yampicture.rental.logic.paid
import com.yampicture.rental.notify.AlertWorker
import com.yampicture.rental.notify.Notifier
import com.yampicture.rental.util.Prefs
import com.yampicture.rental.util.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val prefs = Prefs(app)

    val settings = MutableStateFlow(prefs.load())

    val equipment: StateFlow<List<Equipment>> =
        db.equipment().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customers: StateFlow<List<Customer>> =
        db.customers().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rentals: StateFlow<List<RentalFull>> =
        db.rentals().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ledger: StateFlow<List<LedgerEntry>> =
        db.ledger().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Ticks every 30 seconds so statuses ("overdue by 2h") stay live while the app is open. */
    val now = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                now.value = System.currentTimeMillis()
            }
        }
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(change: (Settings) -> Settings) {
        val next = change(settings.value)
        settings.value = next
        prefs.save(next)
    }

    // ---------------------------------------------------------------- rentals

    /**
     * Creates a rental (finding or creating the customer), and records the deposit as income.
     * [onDone] receives the new rental id.
     */
    fun createRental(
        customerName: String,
        phone: String,
        equipmentIds: List<Long>,
        startAt: Long,
        days: Int,
        deposit: Long,
        notes: String,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val rentalId = db.withTransaction {
                val name = customerName.trim()
                val ph = phone.trim()
                val existing = (if (ph.isNotEmpty()) db.customers().findByPhone(ph) else null)
                    ?: db.customers().findByName(name)
                val customerId = if (existing != null) {
                    if (existing.phone.isBlank() && ph.isNotEmpty()) {
                        db.customers().update(existing.copy(phone = ph))
                    }
                    existing.id
                } else {
                    db.customers().insert(Customer(name = name, phone = ph))
                }

                val dueAt = startAt + days * Billing.DAY_MS
                val id = db.rentals().insert(
                    Rental(
                        customerId = customerId,
                        startAt = startAt,
                        days = days,
                        dueAt = dueAt,
                        notes = notes.trim(),
                    ),
                )
                val all = equipment.value.filter { it.id in equipmentIds }
                db.rentals().insertItems(
                    all.map {
                        RentalItem(
                            rentalId = id,
                            equipmentId = it.id,
                            name = it.name,
                            nameAm = it.nameAm,
                            dailyRate = it.dailyRate,
                        )
                    },
                )
                if (deposit > 0) {
                    db.ledger().insert(
                        LedgerEntry(
                            type = Types.INCOME,
                            category = Cat.DEPOSIT,
                            amount = deposit,
                            at = System.currentTimeMillis(),
                            rentalId = id,
                        ),
                    )
                }
                id
            }
            onDone(rentalId)
        }
    }

    fun addPayment(rentalId: Long, amount: Long, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            db.ledger().insert(
                LedgerEntry(
                    type = Types.INCOME,
                    category = Cat.PAYMENT,
                    amount = amount,
                    at = System.currentTimeMillis(),
                    note = note.trim(),
                    rentalId = rentalId,
                ),
            )
        }
    }

    fun refund(rentalId: Long, amount: Long) {
        if (amount <= 0) return
        viewModelScope.launch {
            db.ledger().insert(
                LedgerEntry(
                    type = Types.EXPENSE,
                    category = Cat.REFUND,
                    amount = amount,
                    at = System.currentTimeMillis(),
                    rentalId = rentalId,
                ),
            )
        }
    }

    /**
     * Marks the rental as returned now and freezes the late days. When [collectBalance] is set, whatever
     * is still owed at this exact moment is recorded as a payment.
     */
    fun returnRental(rentalId: Long, collectBalance: Boolean, returnNotes: String) {
        viewModelScope.launch {
            val f = db.rentals().getFull(rentalId) ?: return@launch
            if (f.rental.returnedAt != null) return@launch
            val at = System.currentTimeMillis()
            val extra = f.extraDays(at, settings.value.graceMs)
            val owed = f.dailyTotal * (f.rental.days + extra) - f.paid
            db.withTransaction {
                db.rentals().update(
                    f.rental.copy(returnedAt = at, finalExtraDays = extra, returnNotes = returnNotes.trim()),
                )
                if (collectBalance && owed > 0) {
                    db.ledger().insert(
                        LedgerEntry(
                            type = Types.INCOME,
                            category = Cat.PAYMENT,
                            amount = owed,
                            at = at,
                            rentalId = rentalId,
                        ),
                    )
                }
            }
            Notifier.cancelFor(app, rentalId)
            now.value = System.currentTimeMillis()
        }
    }

    /**
     * Changes the booked number of days. Returns the name of a customer whose booking blocks the
     * new period (and does nothing), or null on success.
     */
    suspend fun extendRental(rentalId: Long, newDays: Int): String? {
        val f = db.rentals().getFull(rentalId) ?: return null
        if (newDays < 1) return null
        val r = f.rental
        val newDue = r.startAt + newDays * Billing.DAY_MS
        val all = rentals.value
        for (item in f.items) {
            val blocker = Availability.conflict(all, item.equipmentId, r.startAt, newDue, System.currentTimeMillis(), rentalId)
            if (blocker != null) return blocker.customer.name
        }
        db.rentals().update(
            r.copy(days = newDays, dueAt = newDue, dueNotified = false, warnedIndex = 0, addedNotifiedIndex = 0),
        )
        Notifier.cancelFor(app, rentalId)
        AlertWorker.runNow(app)
        return null
    }

    fun deleteRental(rentalId: Long) {
        viewModelScope.launch {
            db.withTransaction {
                db.ledger().deleteForRental(rentalId)
                db.rentals().delete(rentalId)
            }
            Notifier.cancelFor(app, rentalId)
        }
    }

    // ---------------------------------------------------------------- ledger

    fun addLedger(type: String, category: String, amount: Long, at: Long, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            db.ledger().insert(
                LedgerEntry(type = type, category = category, amount = amount, at = at, note = note.trim()),
            )
        }
    }

    fun deleteLedger(id: Long) {
        viewModelScope.launch { db.ledger().delete(id) }
    }

    // ---------------------------------------------------------------- equipment

    fun saveEquipment(id: Long, name: String, nameAm: String, rate: Long, active: Boolean) {
        viewModelScope.launch {
            if (id == 0L) {
                val order = (equipment.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
                db.equipment().insert(
                    Equipment(name = name.trim(), nameAm = nameAm.trim(), dailyRate = rate, active = active, sortOrder = order),
                )
            } else {
                val old = equipment.value.firstOrNull { it.id == id } ?: return@launch
                db.equipment().update(old.copy(name = name.trim(), nameAm = nameAm.trim(), dailyRate = rate, active = active))
            }
        }
    }
}
