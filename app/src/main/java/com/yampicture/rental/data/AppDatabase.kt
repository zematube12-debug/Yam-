package com.yampicture.rental.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Equipment::class, Customer::class, Rental::class, RentalItem::class, LedgerEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipment(): EquipmentDao
    abstract fun customers(): CustomerDao
    abstract fun rentals(): RentalDao
    abstract fun ledger(): LedgerDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yam_rental.db",
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            seed(db)
                        }
                    })
                    .build()
                    .also { instance = it }
            }

        /** The studio's starting price list (per day, ETB). Editable later in the Equipment screen. */
        private fun seed(db: SupportSQLiteDatabase) {
            val items = listOf(
                Triple("Camera", "ካሜራ", 4000),
                Triple("Gimbal", "ጊምባል", 1500),
                Triple("Godox", "ጎዶክስ", 1000),
                Triple("Amaran 1", "አማራን 1", 1000),
                Triple("Amaran 2", "አማራን 2", 1000),
                Triple("Softbox", "ሶፍትቦክስ", 500),
                Triple("Tripod", "ትራይፓድ", 500),
            )
            items.forEachIndexed { index, (name, am, rate) ->
                db.execSQL(
                    "INSERT INTO equipment (name, nameAm, dailyRate, active, sortOrder) VALUES (?, ?, ?, 1, ?)",
                    arrayOf<Any>(name, am, rate, index),
                )
            }
        }
    }
}
