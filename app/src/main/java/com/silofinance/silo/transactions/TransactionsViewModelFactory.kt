package com.silofinance.silo.transactions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.TransactionDbDao


class TransactionsViewModelFactory (
    private val accountDataSource: AccountDbDao,
    private val categoryDataSource: CategoryDbDao,
    private val transactionDataSource: TransactionDbDao,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            return TransactionsViewModel(accountDataSource, categoryDataSource, transactionDataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}