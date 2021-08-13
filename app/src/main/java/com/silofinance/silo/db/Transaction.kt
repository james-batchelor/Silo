package com.silofinance.silo.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.time.LocalDate


@Parcelize  // So that Edit<Income/Expense/Transfer>Dialog's can use args.putParcelable(KEY, transaction)
@Entity(tableName = "t_table")
data class Transaction(

    @PrimaryKey(autoGenerate = true)  // Generates the next never-generated-before Long only when the Transaction is inserted into t_table
    val tId: Long = 0L,

    @ColumnInfo(name = "t_type")
    var tType: Int = 2,  // 1 is income, 2 is expense, 3 is transfer

    @ColumnInfo(name = "t_date")
    var tDate: Long = LocalDate.now().toEpochDay(),

    @ColumnInfo(name = "t_account")
    var tAccount: Long = 0,  // -9 for deleted account

    @ColumnInfo(name = "t_category")
    var tCategory: Long = 0,  // -9 for deleted category, -1 for income, -4 for tbb
    // If tType == 3 ie we have a transfer, then this holds the ID of the account the money is transferred to.

    @ColumnInfo(name = "t_amount")
    var tAmount: Double = 0.0,

    @ColumnInfo(name = "t_note")
    var tNote: String = "",

    @ColumnInfo(name = "t_cleared")
    var tCleared: Boolean = false
) : Parcelable