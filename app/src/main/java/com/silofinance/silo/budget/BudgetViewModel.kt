package com.silofinance.silo.budget

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


class BudgetViewModel(accountDataSource: AccountDbDao, categoryDataSource: CategoryDbDao, transactionDataSource: TransactionDbDao, application: Application) :
    AndroidViewModel(application) {

    private val app = application
    private val accountDbDao = accountDataSource  // Used to access the AccountDbDao
    private val categoryDbDao = categoryDataSource  // Used to access the CategoryDbDao
    private val transactionDbDao = transactionDataSource  // Used to access the TransactionDbDao

    val activeCategories = categoryDbDao.getActive()  // LiveData<List<Category>> reference to the active categories
    val hiddenCategories = categoryDbDao.getHidden()  // LiveData<List<Category>> reference to the hidden categories
    val highestIdActiveAccount = accountDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest aId
    val highestIdActiveCategory = categoryDbDao.getHighestActiveId()  // LiveData<Long> reference to the highest cId


    private val _navigateToAddExpense = MutableLiveData<Boolean>()  // Signals to navigate to add expense
    val navigateToAddExpense  // Global val accessible by the fragment which can only read the MutableLiveData, not modify it
        get() = _navigateToAddExpense
    fun onFabClicked() { _navigateToAddExpense.value = true }
    fun onNavigatedToNewt() { _navigateToAddExpense.value = false }

    private val _moveMoneyFromTbbDialog = MutableLiveData<Boolean>()  // Signals to create and show a MoveMoneyDialog, moving from tbb
    val moveMoneyFromTbbDialog  // Global val accessible by the fragment which can only read the MutableLiveData, not modify it
        get() = _moveMoneyFromTbbDialog
    fun onTbbClicked() { _moveMoneyFromTbbDialog.value = true }
    fun onOpenedTbbToCategory() { _moveMoneyFromTbbDialog.value = false }


    /** Moves money from the category.cAllocated from one category to another. Catches if the 'from category' is tbb and handles this too */
    fun moveTheMoney(fromCId: Long, toCategory: Category, amount: Double) { viewModelScope.launch { susMoveTheMoney(fromCId, toCategory, amount) } }
    private suspend fun susMoveTheMoney(fromCId: Long, toCategory: Category, amount: Double) {
        withContext(Dispatchers.IO) {
            if (fromCId != -4L) {  // So we are NOT moving from tbb
                val fromCategory = categoryDbDao.get(fromCId)
                fromCategory.cAvailable -= amount
                categoryDbDao.update(fromCategory)
            }
            if (toCategory.cId != -4L) {  // So we are NOT moving to tbb
                toCategory.cAvailable += amount
                categoryDbDao.update(toCategory)
            }
            if (fromCId == -4L) {  // So we ARE moving FROM tbb
                val updateAllocatedCategory = categoryDbDao.get(toCategory.cId)
                updateAllocatedCategory.cAllocated = amount
                categoryDbDao.update(updateAllocatedCategory)
            }
        }
    }

    /** Create a new category, and everything that goes with that */
    fun insertCategory(newCategory: Category) { viewModelScope.launch { susInsertCategory(newCategory) } }
    private suspend fun susInsertCategory(newCategory: Category) {
        withContext(Dispatchers.IO) {
            categoryDbDao.insert(newCategory)
            val category = categoryDbDao.getOneByIdDesc()  // Because the category was just added, this holds its true cId
            category.cOrder = category.cId
            categoryDbDao.update(category)

            // Put the categories' emoji into the category emoji shared preferences
            val cEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            cEmojiSharedPref.edit().putString(categoryDbDao.getOneByIdDesc().cId.toString(), newCategory.cEmoji).apply()
        }
    }

    /** Updates a category with a new emoji and name. Replaces the emoji for the account in the cEmojiSharedPref too. */
    fun updateCategoryEmojiName(cId: Long, cEmoji: String, cName: String) { viewModelScope.launch { susUpdateCategoryEmojiName(cId, cEmoji, cName) } }
    private suspend fun susUpdateCategoryEmojiName(cId: Long, cEmoji: String, cName: String) {
        withContext(Dispatchers.IO) {
            val category: Category = categoryDbDao.get(cId)
            category.cEmoji = cEmoji
            category.cName = cName
            categoryDbDao.update(category)

            val cEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            cEmojiSharedPref.edit().putString(cId.toString(), cEmoji).apply()
        }
    }

    /** Flips cActive between true and false, hiding or activating the category */
    fun categoryFlipActive(category: Category) { viewModelScope.launch { susCategoryFlipActive(category) } }
    private suspend fun susCategoryFlipActive(category: Category) {
        withContext(Dispatchers.IO) {
            category.cActive = !category.cActive
            category.cSelected = false  // Prevents the user somehow getting two selected categories
            categoryDbDao.update(category)
        }
    }

    /** Swap the order of the two categories */
    fun categoriesSwapOrder(fromCId: Long, swapWithCId: Long) { viewModelScope.launch { susCategoriesSwapOrder(fromCId, swapWithCId) } }
    private suspend fun susCategoriesSwapOrder(fromCId: Long, swapWithCId: Long) {
        withContext(Dispatchers.IO) {
            val fromCategory = categoryDbDao.get(fromCId)
            val swapWithCategory = categoryDbDao.get(swapWithCId)

            val fromOrder = fromCategory.cOrder

            fromCategory.cOrder = swapWithCategory.cOrder
            swapWithCategory.cOrder = fromOrder

            categoryDbDao.update(fromCategory)
            categoryDbDao.update(swapWithCategory)
        }
    }

    /** Merges fromCategory to toCategory, and everything that goes with that */
    fun mergeCategory(fromCId: Long, toCId: Long) { viewModelScope.launch { susMergeCategory(fromCId, toCId) } }
    private suspend fun susMergeCategory(fromCId: Long, toCId: Long) {
        withContext(Dispatchers.IO) {
            val fromCategory = categoryDbDao.get(fromCId)
            val toCategory = categoryDbDao.get(toCId)

            // Need to change the tCategory of ALL transactions to toCId if they used to have tCategory == fromCId
            val allTransactionsList = transactionDbDao.getAllList()
            for (transaction in allTransactionsList) {
                if (transaction.tCategory == fromCId) {
                    transaction.tCategory = toCId
                    transactionDbDao.update(transaction)
                }
            }

            toCategory.cAvailable += fromCategory.cAvailable
            toCategory.cAllocated += fromCategory.cAllocated
            toCategory.cPrevious += fromCategory.cPrevious
            categoryDbDao.update(toCategory)

            categoryDbDao.delete(fromCId)

            // Remove the fromCategory's emoji from the category emoji shared preferences
            val cEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            cEmojiSharedPref.edit().remove(fromCId.toString()).apply()
        }
    }

    /** Deletes a category, and everything that goes with that */
    fun deleteCategory(cId: Long) { viewModelScope.launch { susDeleteCategory(cId) } }
    private suspend fun susDeleteCategory(cId: Long) {
        withContext(Dispatchers.IO) {
            // Need to change the tCategory of ALL transactions to -9 if they used to have tCategory == cId
            val allTransactionsList = transactionDbDao.getAllList()  // Get a reference to all transactions in the form of List<Transaction>
            for (transaction in allTransactionsList) {
                if (transaction.tCategory == cId) {
                    transaction.tCategory = -9  // -9 for deleted category
                    transactionDbDao.update(transaction)
                }
            }

            val category = categoryDbDao.get(cId)
            categoryDbDao.delete(cId)

            // Add what's available in the category to tbb
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb += category.cAvailable  // Now that you can't hide categories with ~zero cAvailable, this SHOULDN'T do anything. But there's no harm in leaving this in
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()

            // Remove the categories' emoji from the category emoji shared preferences
            val cEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            cEmojiSharedPref.edit().remove(cId.toString()).apply()
        }
    }
}