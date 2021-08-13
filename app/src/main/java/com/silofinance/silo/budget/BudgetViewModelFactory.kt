package com.silofinance.silo.budget

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.TransactionDbDao


class BudgetViewModelFactory (
    private val accountDataSource: AccountDbDao,
    private val categoryDataSource: CategoryDbDao,
    private val transactionDataSource: TransactionDbDao,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            return BudgetViewModel(accountDataSource, categoryDataSource, transactionDataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}