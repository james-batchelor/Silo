package com.silofinance.silo.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// https://developer.android.com/courses/extras/sql-primer  - Guide
@Dao
interface CategoryDbDao {

    /** Insert a category into the database */
    @Insert
    fun insert(category: Category)

    /** When updating a row with a value already set in a column, replaces the old value with the new one */
    @Update
    fun update(category: Category)

    /** Get the category with the given cId */
    @Query("SELECT * from c_table WHERE cId = :key")
    fun get(key: Long): Category

    /** Get the category with the highest cId */
    @Query("SELECT * from c_table ORDER BY cId DESC LIMIT 1")
    fun getOneByIdDesc(): Category

    /** Delete the category with the given cId */
    @Query("DELETE FROM c_table WHERE cId = :key")
    fun delete(key: Long)

    /** Deletes all values from the table. This does not delete the table, only its contents */
    @Query("DELETE FROM c_table")
    fun clear()

    /** Returns all active categories, sorted by c_order in ascending order */
    @Query("SELECT * FROM c_table WHERE c_active = 1 ORDER BY c_order ASC")
    fun getActive(): LiveData<List<Category>>

    /** Selects and returns (as a list) all active categories, sorted by c_order in ascending order */
    @Query("SELECT * FROM c_table WHERE c_active = 1 ORDER BY c_order ASC")
    fun getActiveList(): List<Category>

    /** Returns all hidden categories, sorted by c_order in ascending order */
    @Query("SELECT * FROM c_table WHERE c_active = 0 ORDER BY c_order ASC")
    fun getHidden(): LiveData<List<Category>>

    /** Returns all hidden category cId's, sorted by cId in ascending order */
    @Query("SELECT cId FROM c_table WHERE c_active = 0 ORDER BY cId ASC")
    fun getHiddenIds(): LiveData<List<Long>>

    /** Gets the highest active cId */
    @Query("SELECT cId FROM c_table WHERE c_active = 1 ORDER BY cId DESC LIMIT 1")
    fun getHighestActiveId(): LiveData<Long>
}
