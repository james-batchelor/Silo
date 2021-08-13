package com.silofinance.silo.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.db.TransactionDbDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ExpenseViewModel(accountDataSource: AccountDbDao, categoryDataSource: CategoryDbDao, transactionDataSource: TransactionDbDao, application: Application) :
    AndroidViewModel(application) {

    private val accountDbDao = accountDataSource  // Hold a reference to the AccountDb via the AccountDbDao
    private val categoryDbDao = categoryDataSource  // Hold a reference to the CategoryDb via the CategoryDbDao
    private val transactionDbDao = transactionDataSource  // Hold a reference to the TransactionDb via the TransactionDbDao

    val activeAccounts = accountDbDao.getActive()  // LiveData<List<Account>> reference to the active accounts
    val activeCategories = categoryDbDao.getActive()  // LiveData<List<Category>> reference to the active categories


    /** Select an account, and unselect all others */
    fun ensureSelections() { viewModelScope.launch { susEnsureSelections() } }
    private suspend fun susEnsureSelections() {
        withContext(Dispatchers.IO) {
            val allActiveAccountsList = accountDbDao.getActiveList()
            var accountIsSelected = false
            for (account in allActiveAccountsList) {
                if (account.aSelected) {
                    accountIsSelected = true
                }
            }
            if (!accountIsSelected) {
                val account = allActiveAccountsList[0]
                account.aSelected = true
                accountDbDao.update(account)
            }

            val allActiveCategoriesList = categoryDbDao.getActiveList()
            var categoryIsSelected = false
            for (category in allActiveCategoriesList) {
                if (category.cSelected) {
                    categoryIsSelected = true
                }
            }
            if (!categoryIsSelected) {
                val category = allActiveCategoriesList[0]
                category.cSelected = true
                categoryDbDao.update(category)
            }
        }
    }

    /** Select an account, and unselect all others */
    fun selectAccount(aId: Long) { viewModelScope.launch { susSelectAccount(aId) } }
    private suspend fun susSelectAccount(aId: Long) {
        withContext(Dispatchers.IO) {
            // Set aSelected of the previously selected account to false, and select the newly selected account
            val allActiveAccountsList = accountDbDao.getActiveList()
            for (account in allActiveAccountsList) {
                if (account.aSelected) {
                    account.aSelected = false
                    accountDbDao.update(account)
                }
                if (account.aId == aId) {
                    account.aSelected = true
                    accountDbDao.update(account)
                }
            }
        }
    }

    /** Select a category, and unselect all others */
    fun selectCategory(cId: Long) { viewModelScope.launch { susSelectCategory(cId) } }
    private suspend fun susSelectCategory(cId: Long) {
        withContext(Dispatchers.IO) {
            // Set cSelected of the previously selected category to false, and select the newly selected category
            val allActiveCategoriesList = categoryDbDao.getActiveList()
            for (category in allActiveCategoriesList) {
                if (category.cSelected) {
                    category.cSelected = false
                    categoryDbDao.update(category)
                }
                if (category.cId == cId) {
                    category.cSelected = true
                    categoryDbDao.update(category)
                }
            }
        }
    }

    /** Create an expense, and everything that goes with that */
    fun insertExpense(newExpense: Transaction) { viewModelScope.launch { susInsertExpense(newExpense) } }
    private suspend fun susInsertExpense(newExpense: Transaction) {
        withContext(Dispatchers.IO) {
            // Find the selected account and subtract the expense from its aCleared or aPending based on tCleared
            val allActiveAccountsList = accountDbDao.getActiveList()
            for (account in allActiveAccountsList) {
                if (account.aSelected) {
                    newExpense.tAccount = account.aId  // Give the expense its account

                    if (newExpense.tCleared) {
                        account.aCleared += newExpense.tAmount
                    } else {
                        account.aPending += newExpense.tAmount
                    }
                    accountDbDao.update(account)
                    break
                }
            }

            // Find the selected category and add the expense (a negative number) from its cAvailable
            val allActiveCategoriesList = categoryDbDao.getActiveList()
            for (category in allActiveCategoriesList) {
                if (category.cSelected) {
                    newExpense.tCategory = category.cId  // Give the expense its category

                    category.cAvailable += newExpense.tAmount
                    categoryDbDao.update(category)
                    break
                }
            }

            transactionDbDao.insert(newExpense)
        }
    }
}