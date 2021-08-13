package com.silofinance.silo.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class TransactionDb : RoomDatabase() {

    /** Connects the database to the DAO */
    abstract val transactionDbDao: TransactionDbDao

    /** Define a companion object, this allows adding functions in this abstract class */
    companion object {

        /** INSTANCE will keep a reference to any database returned via getInstance */
        @Volatile  //The value of a volatile variable will never be cached, and all writes and reads will be done to and from the main memory. It means that changes made by one thread to shared data are visible to other threads.
        private var INSTANCE: TransactionDb? = null

        /** If a database has already been retrieved, the previous database will be returned. Otherwise, create a new database.
          * This function is threadsafe, and callers should cache the result for multiple database calls to avoid overhead. */
        fun getInstance(context: Context): TransactionDb {
            synchronized(this) {  // Multiple threads can ask for the database at the same time so ensure we only initialize it once by using synchronized. Only one thread may enter a synchronized block at a time
                var instance = INSTANCE  // Do this to use smart cast (only available to local variables)

                if (instance == null) {  // If instance is null then make a new database instance
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TransactionDb::class.java,
                        "transaction_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance  // Return instance; smart cast to be non-null
            }
        }
    }
}
