package com.silofinance.silo.transactions

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.silofinance.silo.R
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.db.TransactionDbDao
import com.silofinance.silo.getDouble
import com.silofinance.silo.putDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TransactionsViewModel(accountDataSource: AccountDbDao, categoryDataSource: CategoryDbDao, transactionDataSource: TransactionDbDao, application: Application) :
    AndroidViewModel(application) {

    private val app = application
    private val accountDbDao = accountDataSource  // Used to access the AccountDbDao
    private val categoryDbDao = categoryDataSource  // Used to access the CategoryDbDao
    private val transactionDbDao = transactionDataSource  // Used to access the TransactionDbDao

    val activeAccounts = accountDbDao.getActive()  // LiveData<List<Account>> reference to the active accounts
    val activeCategories = categoryDbDao.getActive()  // LiveData<List<Category>> reference to the active categories
    val allTransactions = transactionDbDao.getAll()  // LiveData<List<Transaction>> reference to all transactions
    val hiddenAccountIds = accountDbDao.getHiddenIds()  // LiveData<List<Long>> reference to the hidden account aId's
    val hiddenCategoryIds = categoryDbDao.getHiddenIds()  // LiveData<List<Long>> reference to the hidden category cId's
    val highestIdActiveAccount = accountDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest aId
    val highestIdActiveCategory = categoryDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest cId


    private val _navigateToAddExpense = MutableLiveData<Boolean>()  // Signals to navigate to ExpenseFragment
    val navigateToAddExpense  // Global val accessible by the fragment which can only read the MutableLiveData, not modify it
        get() = _navigateToAddExpense
    fun onFabClicked() { _navigateToAddExpense.value = true }
    fun onNavigatedToNewt() { _navigateToAddExpense.value = false }


    /** Flips the state of the transaction between cleared and pending */
    fun flipCleared(tId: Long) { viewModelScope.launch { susFlipCleared(tId) } }
    private suspend fun susFlipCleared(tId: Long) {
        withContext(Dispatchers.IO) {
            val transaction: Transaction = transactionDbDao.get(tId)

            // For non transfers: update the values in the account (if the id isn't -9 ie deleted)
            if (transaction.tType != 3) {
                if (transaction.tAccount != -9L) {
                    val account = accountDbDao.get(transaction.tAccount)
                    if (transaction.tCleared) {
                        account.aCleared -= transaction.tAmount
                        account.aPending += transaction.tAmount
                    } else if (!transaction.tCleared) {
                        account.aCleared += transaction.tAmount
                        account.aPending -= transaction.tAmount
                    }
                    accountDbDao.update(account)
                }
            }

            // For transfers: update the values in the 'from' and 'to' accounts (if the ids aren't -9 ie deleted)
            if (transaction.tType == 3) {
                if (transaction.tAccount != -9L) {
                    val fromAccount = accountDbDao.get(transaction.tAccount)
                    if (transaction.tCleared) {
                        fromAccount.aCleared += transaction.tAmount
                        fromAccount.aPending -= transaction.tAmount
                    } else if (!transaction.tCleared) {
                        fromAccount.aCleared -= transaction.tAmount
                        fromAccount.aPending += transaction.tAmount
                    }
                    accountDbDao.update(fromAccount)
                }
                if (transaction.tCategory != -9L) {
                    val toAccount = accountDbDao.get(transaction.tCategory)
                    if (transaction.tCleared) {
                        toAccount.aCleared -= transaction.tAmount
                        toAccount.aPending += transaction.tAmount
                    } else if (!transaction.tCleared) {
                        toAccount.aCleared += transaction.tAmount
                        toAccount.aPending -= transaction.tAmount
                    }
                    accountDbDao.update(toAccount)
                }
            }

            transaction.tCleared = !transaction.tCleared
            transactionDbDao.update(transaction)
        }
    }

    /** Updates the (income) transaction in the TransactionDb */
    fun editIncome(oldIncome: Transaction, newIncome: Transaction) { viewModelScope.launch { susEditIncome(oldIncome, newIncome) } }
    private suspend fun susEditIncome(oldIncome: Transaction, newIncome: Transaction) {
        withContext(Dispatchers.IO) {
            if (oldIncome.tAccount != -9L) {  // If the account hasn't been deleted
                val oldAccount = accountDbDao.get(oldIncome.tAccount)
                when (oldIncome.tCleared) {
                    true -> oldAccount.aCleared -= oldIncome.tAmount
                    false -> oldAccount.aPending -= oldIncome.tAmount
                }
                accountDbDao.update(oldAccount)
            }

            // No deleted check, because you can't selected deleted accounts/categories in the dialog spinners
            val newAccount = accountDbDao.get(newIncome.tAccount)  // If the account wasn't changed by the user then this is the same as oldAccount
            when (newIncome.tCleared) {
                true -> newAccount.aCleared += newIncome.tAmount
                false -> newAccount.aPending += newIncome.tAmount
            }
            accountDbDao.update(newAccount)

            transactionDbDao.update(newIncome)  // newIncome.tId == oldIncome.tId, so this overwrites oldIncome with newIncome

            // Update tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb += (newIncome.tAmount - oldIncome.tAmount)
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()
        }
    }

    /** Updates the (expense) transaction in the TransactionDb */
    fun editExpense(oldExpense: Transaction, newExpense: Transaction) { viewModelScope.launch { susEditExpense(oldExpense, newExpense) } }
    private suspend fun susEditExpense(oldExpense: Transaction, newExpense: Transaction) {
        withContext(Dispatchers.IO) {
            if (oldExpense.tAccount != -9L) {  // If the account hasn't been deleted
                val oldAccount = accountDbDao.get(oldExpense.tAccount)
                when (oldExpense.tCleared) {
                    true -> oldAccount.aCleared -= oldExpense.tAmount
                    false -> oldAccount.aPending -= oldExpense.tAmount
                }
                accountDbDao.update(oldAccount)
            }

            if (oldExpense.tCategory != -9L) {  // If the category hasn't been deleted
                val oldCategory = categoryDbDao.get(oldExpense.tCategory)
                oldCategory.cAvailable -= oldExpense.tAmount
                categoryDbDao.update(oldCategory)
            }

            // No deleted checks, because you can't selected deleted accounts/categories in the dialog spinners
            val newAccount = accountDbDao.get(newExpense.tAccount)
            when (newExpense.tCleared) {
                true -> newAccount.aCleared += newExpense.tAmount
                false -> newAccount.aPending += newExpense.tAmount
            }
            accountDbDao.update(newAccount)

            val newCategory = categoryDbDao.get(newExpense.tCategory)
            newCategory.cAvailable += newExpense.tAmount
            categoryDbDao.update(newCategory)

            transactionDbDao.update(newExpense)
        }
    }

    /** Updates the (transfer) transaction in the TransactionDb */
    fun editTransfer(oldTransfer: Transaction, newTransfer: Transaction) { viewModelScope.launch { susEditTransfer(oldTransfer, newTransfer) } }
    private suspend fun susEditTransfer(oldTransfer: Transaction, newTransfer: Transaction) {
        withContext(Dispatchers.IO) {
            if (oldTransfer.tAccount != -9L) {  // If the account hasn't been deleted
                val oldFromAccount = accountDbDao.get(oldTransfer.tAccount)
                when (oldTransfer.tCleared) {
                    true -> oldFromAccount.aCleared += oldTransfer.tAmount
                    false -> oldFromAccount.aPending += oldTransfer.tAmount
                }
                accountDbDao.update(oldFromAccount)
            }

            if (oldTransfer.tCategory != -9L) {  // If the account hasn't been deleted
                val oldToAccount = accountDbDao.get(oldTransfer.tCategory)
                when (oldTransfer.tCleared) {
                    true -> oldToAccount.aCleared -= oldTransfer.tAmount
                    false -> oldToAccount.aPending -= oldTransfer.tAmount
                }
                accountDbDao.update(oldToAccount)
            }

            // No deleted checks, because you can't selected deleted accounts in the dialog spinners
            val newFromAccount = accountDbDao.get(newTransfer.tAccount)
            when (newTransfer.tCleared) {
                true -> newFromAccount.aCleared -= newTransfer.tAmount
                false -> newFromAccount.aPending -= newTransfer.tAmount
            }
            accountDbDao.update(newFromAccount)

            // No deleted checks, because you can't selected deleted accounts in the dialog spinners
            val newToAccount = accountDbDao.get(newTransfer.tCategory)
            when (newTransfer.tCleared) {
                true -> newToAccount.aCleared += newTransfer.tAmount
                false -> newToAccount.aPending += newTransfer.tAmount
            }
            accountDbDao.update(newToAccount)

            transactionDbDao.update(newTransfer)  // newTransfer.tId == oldTransfer.tId, so this overwrites oldTransfer with newTransfer
        }
    }

    /** Deletes an income from TransactionDb */
    fun deleteIncome(tId: Long) { viewModelScope.launch { susDeleteIncome(tId) } }
    private suspend fun susDeleteIncome(tId: Long) {
        withContext(Dispatchers.IO) {
            val income = transactionDbDao.get(tId)

            if (income.tAccount != -9L) {  // If the account hasn't been deleted
                val account = accountDbDao.get(income.tAccount)
                when (income.tCleared) {
                    true -> account.aCleared -= income.tAmount
                    false -> account.aPending -= income.tAmount
                }
                accountDbDao.update(account)
            }

            transactionDbDao.delete(tId)

            // Subtract the income amount from tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb -= income.tAmount
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()
        }
    }

    /** Deletes an expense from TransactionDb */
    fun deleteExpense(tId: Long) { viewModelScope.launch { susDeleteExpense(tId) } }
    private suspend fun susDeleteExpense(tId: Long) {
        withContext(Dispatchers.IO) {
            val expense = transactionDbDao.get(tId)

            if (expense.tAccount != -9L) {  // If the account hasn't been deleted
                val account = accountDbDao.get(expense.tAccount)
                when (expense.tCleared) {
                    true -> account.aCleared -= expense.tAmount
                    false -> account.aPending -= expense.tAmount
                }
                accountDbDao.update(account)
            }

            if (expense.tCategory != -9L) {  // If the category hasn't been deleted
                val category = categoryDbDao.get(expense.tCategory)
                category.cAvailable -= expense.tAmount
                categoryDbDao.update(category)
            }

            transactionDbDao.delete(tId)
        }
    }

    /** Deletes a transfer from TransactionDb */
    fun deleteTransfer(tId: Long) { viewModelScope.launch { susDeleteTransfer(tId) } }
    private suspend fun susDeleteTransfer(tId: Long) {
        withContext(Dispatchers.IO) {
            val transfer = transactionDbDao.get(tId)

            if (transfer.tAccount != -9L) {  // If the account hasn't been deleted
                val fromAccount = accountDbDao.get(transfer.tAccount)
                when (transfer.tCleared) {
                    true -> fromAccount.aCleared += transfer.tAmount
                    false -> fromAccount.aPending += transfer.tAmount
                }
                accountDbDao.update(fromAccount)
            }

            if (transfer.tCategory != -9L) {  // If the account hasn't been deleted
                val toAccount = accountDbDao.get(transfer.tCategory)
                when (transfer.tCleared) {
                    true -> toAccount.aCleared -= transfer.tAmount
                    false -> toAccount.aPending -= transfer.tAmount
                }
                accountDbDao.update(toAccount)
            }

            transactionDbDao.delete(tId)
        }
    }
}