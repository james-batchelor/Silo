package com.silofinance.silo.accounts

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.TransactionDbDao


class AccountsViewModelFactory (
    private val accountDataSource: AccountDbDao,
    private val categoryDataSource: CategoryDbDao,
    private val transactionDataSource: TransactionDbDao,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountsViewModel::class.java)) {
            return AccountsViewModel(accountDataSource, categoryDataSource, transactionDataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}