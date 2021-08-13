package com.silofinance.silo.about

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silofinance.silo.R
import com.silofinance.silo.db.AccountDbDao
import com.silofinance.silo.db.CategoryDbDao
import com.silofinance.silo.db.TransactionDbDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AboutViewModel(accountDataSource: AccountDbDao, categoryDataSource: CategoryDbDao, transactionDataSource: TransactionDbDao, application: Application) :
    AndroidViewModel(application) {

    private val app = application
    private val accountDbDao = accountDataSource  // Used to access the AccountDbDao
    private val categoryDbDao = categoryDataSource  // Used to access the CategoryDbDao
    private val transactionDbDao = transactionDataSource  // Used to access the TransactionDbDao


    /** Clear all databases and shared prefs */
    fun clearAllDatabases() { viewModelScope.launch { susClearAllDatabases() } }
    private suspend fun susClearAllDatabases() {
        withContext(Dispatchers.IO) {
            accountDbDao.clear()
            val aEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            aEmojiSharedPref.edit().clear().apply()

            categoryDbDao.clear()
            val cEmojiSharedPref = app.getSharedPreferences(app.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            cEmojiSharedPref.edit().clear().apply()
            val tbbSharedPref = app.getSharedPreferences(app.getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            tbbSharedPref.edit().clear().apply()

            transactionDbDao.clear()
        }
    }
}