package com.silofinance.silo.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// https://developer.android.com/courses/extras/sql-primer  - Guide
@Dao
interface TransactionDbDao {

    /** Insert a transaction into the database */
    @Insert
    fun insert(transaction: Transaction)

    /** When updating a row with a value already set in a column, replaces the old value with the new one */
    @Update
    fun update(transaction: Transaction)

    /** Get the transaction with the given tId */
    @Query("SELECT * from t_table WHERE tId = :key")
    fun get(key: Long): Transaction

    /** Deletes all values from the table. This does not delete the table, only its contents */
    @Query("DELETE FROM t_table")
    fun clear()

    /** Delete the transaction with the given tId */
    @Query("DELETE FROM t_table WHERE tId = :key")
    fun delete(key: Long)

    /** Selects and returns all rows in the table */
    @Query("SELECT * FROM t_table ORDER BY t_date DESC, t_amount ASC, tId DESC")
    fun getAll(): LiveData<List<Transaction>>

    /** Selects and returns (as a list) all rows in the table. Used when deleting account/categories, ViewModel functions etc */
    @Query("SELECT * FROM t_table ORDER BY t_date DESC")
    fun getAllList(): List<Transaction>
}
