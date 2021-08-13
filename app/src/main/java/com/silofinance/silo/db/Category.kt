package com.silofinance.silo.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "c_table")
data class Category(

    @PrimaryKey(autoGenerate = true)  // Generates the next never-generated-before Long only when the Category is inserted into c_table
    val cId: Long = 0L,  // -9 for deleted category, -1 for income, -4 for tbb (used to populate spinners, never to go in the db)

    @ColumnInfo(name = "c_order")
    var cOrder: Long = 0L,

    @ColumnInfo(name = "c_type") // 1 is normal, 2 is a fund, 3 is a target, 4 is tbb (used to populate spinners, never to go in the db)
    var cType: Int = 1,

    @ColumnInfo(name = "c_emoji")
    var cEmoji: String = "c_emoji",

    @ColumnInfo(name = "c_name")
    var cName: String = "c_name",

    @ColumnInfo(name = "c_available")
    var cAvailable: Double = 0.0,

    @ColumnInfo(name = "c_allocated") // Allocated this month
    var cAllocated: Double = 0.0,

    @ColumnInfo(name = "c_previous") // Allocated last month
    var cPrevious: Double = 0.0,

    @ColumnInfo(name = "c_target")
    var cTarget: Double = 0.0,

    @ColumnInfo(name = "c_deadline")
    var cDeadline: String = "c_deadline",

    @ColumnInfo(name = "c_active")
    var cActive: Boolean = true,

    @ColumnInfo(name = "c_selected")
    var cSelected: Boolean = false
)