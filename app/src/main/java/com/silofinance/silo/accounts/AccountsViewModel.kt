package com.silofinance.silo.accounts

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.silofinance.silo.R
import com.silofinance.silo.db.*
import com.silofinance.silo.getDouble
import com.silofinance.silo.putDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate


class AccountsViewModel(accountDataSource: AccountDbDao, categoryDataSource: CategoryDbDao, transactionDataSource: TransactionDbDao, application: Application) :
    AndroidViewModel(application) {

    private val app = application
    private val accountDbDao = accountDataSource  // Used to access the AccountDbDao
    private val categoryDbDao = categoryDataSource  // Used to access the CategoryDbDao
    private val transactionDbDao = transactionDataSource  // Used to access the TransactionDbDao

    val activeAccounts = accountDbDao.getActive()  // LiveData<List<Account>> reference to the active accounts
    val hiddenAccounts = accountDbDao.getHidden()  // LiveData<List<Account>> reference to the hidden accounts
    val allAccounts = accountDbDao.getAll()  // LiveData<List<Account>> reference to all accounts
    val highestIdActiveAccount = accountDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest aId
    val highestIdActiveCategory = categoryDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest cId


    private val _navigateToAddExpense = MutableLiveData<Boolean>()  // Signals to navigate to add expense
    val navigateToAddExpense  // Global val accessible by the fragment which can only read the MutableLiveData, not modify it
        get() = _navigateToAddExpense
    fun onFabClicked() { _navigateToAddExpense.value = true }
    fun onNavigatedToNewt() { _navigateToAddExpense.value = false }


    /** Create a new account, and everything that goes with that */
    fun insertAccount(newAccount: Account) { viewModelScope.launch { susInsertAccount(newAccount) } }
    private suspend fun susInsertAccount(newAccount: Account) {
        withContext(Dispatchers.IO) {
            accountDbDao.insert(newAccount)  // Add the account first to generate its aId
            val account = accountDbDao.getOneByIdDesc()  // Because the account was just added, this holds its true aId
            account.aOrder = account.aId
            accountDbDao.update(account)

            // Set the starting value of the account by creating an income
            val newIncome = Transaction()
            newIncome.tType = 1  // 1 for income
            newIncome.tDate = LocalDate.now().toEpochDay()
            newIncome.tAccount = account.aId  // Need to use account, not newAccount, because newAccount.aId = 0
            newIncome.tCategory = -1  // -1 for income
            newIncome.tAmount = newAccount.aCleared
            newIncome.tNote = "Starting balance" //todo extract
            newIncome.tCleared = true
            transactionDbDao.insert(newIncome)

            // Put the account's emoji into the account emoji shared preferences
            val aEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            aEmojiSharedPref.edit().putString(newIncome.tAccount.toString(), newAccount.aEmoji).apply()

            // Add the starting balance of the account to tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb += newAccount.aCleared
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()
        }
    }

    /** Create a new income, and everything that goes with that */
    fun insertIncome(newIncome: Transaction) { viewModelScope.launch { susInsertIncome(newIncome) } }
    private suspend fun susInsertIncome(newIncome: Transaction) {
        withContext(Dispatchers.IO) {
            // Add the money to the account
            val account = accountDbDao.get(newIncome.tAccount)
            if (newIncome.tCleared) {
                account.aCleared += newIncome.tAmount
            } else {
                account.aPending += newIncome.tAmount
            }
            accountDbDao.update(account)

            transactionDbDao.insert(newIncome)

            // Add the money to tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb += newIncome.tAmount
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()
        }
    }

    /** Create a new transfer, and everything that goes with that */
    fun insertTransfer(newTransfer: Transaction) { viewModelScope.launch { susInsertTransfer(newTransfer) } }
    private suspend fun susInsertTransfer(newTransfer: Transaction) {
        withContext(Dispatchers.IO) {
            val fromAccount = accountDbDao.get(newTransfer.tAccount)
            val toAccount = accountDbDao.get(newTransfer.tCategory)

            if (newTransfer.tCleared) {
                fromAccount.aCleared -= newTransfer.tAmount
                toAccount.aCleared += newTransfer.tAmount
            } else {
                fromAccount.aPending -= newTransfer.tAmount
                toAccount.aPending += newTransfer.tAmount
            }

            accountDbDao.update(fromAccount)
            accountDbDao.update(toAccount)
            transactionDbDao.insert(newTransfer)
        }
    }

    /** Updates an account with a new emoji and name. Replaces the emoji for the account in the aEmojiSharedPref too. */
    fun updateAccountEmojiName(aId: Long, aEmoji: String, aName: String) { viewModelScope.launch { susUpdateAccountEmojiName(aId, aEmoji, aName) } }
    private suspend fun susUpdateAccountEmojiName(aId: Long, aEmoji: String, aName: String) {
        withContext(Dispatchers.IO) {
            val account: Account = accountDbDao.get(aId)
            account.aEmoji = aEmoji
            account.aName = aName
            accountDbDao.update(account)

            val aEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            aEmojiSharedPref.edit().putString(aId.toString(), aEmoji).apply()
        }
    }

    /** Flips the account.aAutoclear */
    fun flipAutoclear(aId: Long) { viewModelScope.launch { susFlipAutoclear(aId) } }
    private suspend fun susFlipAutoclear(aId: Long) {
        withContext(Dispatchers.IO) {
            val account: Account = accountDbDao.get(aId)
            account.aAutoclear = !account.aAutoclear
            accountDbDao.update(account)
        }
    }

    /** Flips aActive between true and false, hiding or activating the account */
    fun accountFlipActive(account: Account) { viewModelScope.launch { susAccountFlipActive(account) } }
    private suspend fun susAccountFlipActive(account: Account) {
        withContext(Dispatchers.IO) {
            account.aActive = !account.aActive
            account.aSelected = false  // Prevents the user somehow getting two selected accounts
            accountDbDao.update(account)
        }
    }

    /** Swap the order of the two accounts */
    fun accountsSwapOrder(fromAId: Long, swapWithAId: Long) { viewModelScope.launch { susAccountsSwapOrder(fromAId, swapWithAId) } }
    private suspend fun susAccountsSwapOrder(fromAId: Long, swapWithAId: Long) {
        withContext(Dispatchers.IO) {
            val fromAccount = accountDbDao.get(fromAId)
            val swapWithAccount = accountDbDao.get(swapWithAId)

            val fromOrder = fromAccount.aOrder

            fromAccount.aOrder = swapWithAccount.aOrder
            swapWithAccount.aOrder = fromOrder

            accountDbDao.update(fromAccount)
            accountDbDao.update(swapWithAccount)
        }
    }

    /** Merges fromAccount to toAccount, and everything that goes with that */
    fun mergeAccount(fromAId: Long, toAId: Long) { viewModelScope.launch { susMergeAccount(fromAId, toAId) } }
    private suspend fun susMergeAccount(fromAId: Long, toAId: Long) {
        withContext(Dispatchers.IO) {
            val fromAccount = accountDbDao.get(fromAId)
            val toAccount = accountDbDao.get(toAId)

            // Need to change the tAccount of ALL transactions to toAId if they used to have tAccount == fromAId
            val allTransactionsList = transactionDbDao.getAllList()
            for (transaction in allTransactionsList) {
                if (transaction.tAccount == fromAId) {
                    transaction.tAccount = toAId
                    transactionDbDao.update(transaction)
                }
                if (transaction.tType == 3) {  // And if the transaction is a transfer, check if it's transferring to the from account too.
                    if (transaction.tCategory == fromAId) {
                        transaction.tCategory = toAId
                        transactionDbDao.update(transaction)
                    }
                }
            }

            toAccount.aCleared += fromAccount.aCleared
            toAccount.aPending += fromAccount.aPending
            accountDbDao.update(toAccount)

            accountDbDao.delete(fromAId)

            // Remove the fromAccount's emoji from the account emoji shared preferences
            val aEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            aEmojiSharedPref.edit().remove(fromAId.toString()).apply()
        }
    }

    /** Deletes an account, and everything that goes with that */
    fun deleteAccount(aId: Long) { viewModelScope.launch { susDeleteAccount(aId) } }
    private suspend fun susDeleteAccount(aId: Long) {
        withContext(Dispatchers.IO) {
            val account = accountDbDao.get(aId)
            val balance = (account.aCleared + account.aPending)  // Now that you can't hide accounts with ~zero balances, this SHOULD be ~zero. But there's no harm in leaving this in

            // Need to change the tAccount of ALL transactions to -9 if they used to have tAccount == aId
            val allTransactionsList = transactionDbDao.getAllList()
            for (transaction in allTransactionsList) {
                if (transaction.tAccount == aId) {
                    transaction.tAccount = -9  // -9 for deleted account
                    transactionDbDao.update(transaction)
                }
                if (transaction.tType == 3) {  // And if the transaction is a transfer, check if it's transferring to the account too.
                    if (transaction.tCategory == aId) {
                        transaction.tCategory = -9  // -9 for deleted account
                        transactionDbDao.update(transaction)
                    }
                }
            }

            accountDbDao.delete(aId)

            // Subtract the account's balance from tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb -= balance
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()

            // Remove the account's emoji from the account emoji shared preferences
            val aEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            aEmojiSharedPref.edit().remove(aId.toString()).apply()
        }
    }
}