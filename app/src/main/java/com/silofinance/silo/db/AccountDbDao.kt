package com.silofinance.silo.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// https://developer.android.com/courses/extras/sql-primer  - Guide
@Dao
interface AccountDbDao {

    /** Insert an account */
    @Insert
    fun insert(account: Account)

    /** When updating a row with a value already set in a column, replaces the old value with the new one */
    @Update
    fun update(account: Account)

    /** Get the account with the given aId */
    @Query("SELECT * from a_table WHERE aId = :key")
    fun get(key: Long): Account

    /** Get the account with the highest aId */
    @Query("SELECT * from a_table ORDER BY aId DESC LIMIT 1")
    fun getOneByIdDesc(): Account

    /** Delete the account with the given aId */
    @Query("DELETE FROM a_table WHERE aId = :key")
    fun delete(key: Long)

    /** Deletes all values from the table. This does not delete the table, only its contents */
    @Query("DELETE FROM a_table")
    fun clear()

    /** Returns all active accounts, sorted by a_order in ascending order */
    @Query("SELECT * FROM a_table WHERE a_active = 1 ORDER BY a_order ASC")
    fun getActive(): LiveData<List<Account>>

    /** Selects and returns (as a list) all active accounts, sorted by a_order in ascending order */
    @Query("SELECT * FROM a_table WHERE a_active = 1 ORDER BY a_order ASC")
    fun getActiveList(): List<Account>

    /** Returns all hidden accounts, sorted by a_order in ascending order */
    @Query("SELECT * FROM a_table WHERE a_active = 0 ORDER BY a_order ASC")
    fun getHidden(): LiveData<List<Account>>

    /** Returns all hidden accounts, sorted by aId in ascending order */
    @Query("SELECT * FROM a_table ORDER BY aId ASC")
    fun getAll(): LiveData<List<Account>>

    /** Returns all hidden account aId's, sorted by aId in ascending order */
    @Query("SELECT aId FROM a_table WHERE a_active = 0 ORDER BY aId ASC")
    fun getHiddenIds(): LiveData<List<Long>>

    /** Gets the highest active aId */
    @Query("SELECT aId FROM a_table WHERE a_active = 1 ORDER BY aId DESC LIMIT 1")
    fun getHighestActiveId(): LiveData<Long>
}
