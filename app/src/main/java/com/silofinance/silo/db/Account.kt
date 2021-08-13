package com.silofinance.silo.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "a_table")
data class Account (

    @PrimaryKey(autoGenerate = true)  // Generates the next never-generated-before Long only when the Account is inserted into a_table
    val aId: Long = 0L,  // -9 for deleted account

    @ColumnInfo(name = "a_order")
    var aOrder: Long = 0L,

    @ColumnInfo(name = "a_emoji")
    var aEmoji: String = "a_emoji",

    @ColumnInfo(name = "a_name")
    var aName: String = "a_name",

    @ColumnInfo(name = "a_cleared")
    var aCleared: Double = 0.0,

    @ColumnInfo(name = "a_pending")
    var aPending: Double = 0.0,

    @ColumnInfo(name = "a_autoclear")
    var aAutoclear: Boolean = false,

    @ColumnInfo(name = "a_active")
    var aActive: Boolean = true,

    @ColumnInfo(name = "a_selected")
    var aSelected: Boolean = false
)