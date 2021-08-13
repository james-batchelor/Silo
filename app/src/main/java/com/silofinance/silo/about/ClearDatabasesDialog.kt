package com.silofinance.silo.about

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.databinding.DialogClearDatabasesBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class ClearDatabasesDialog : DialogFragment() {

    companion object{
        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance() : ClearDatabasesDialog {
            return ClearDatabasesDialog()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogClearDatabasesBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_clear_databases, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AboutViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AboutViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        binding.dcdClear.setTextColor(Color.LTGRAY)
        binding.dcdSure.setOnClickListener{
            when (binding.dcdSure.isChecked) {
                true -> binding.dcdClear.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                false -> binding.dcdClear.setTextColor(Color.LTGRAY)
            }
        }

        // Clears the accounts, categories and transactions databases. Clears the aEmoji, cEmoji and tbb shared prefs too.
        binding.dcdClear.setOnClickListener{
            if (binding.dcdSure.isChecked) {
                viewModel.clearAllDatabases()
                Toast.makeText(context, "All databases cleared", Toast.LENGTH_SHORT).show()  // todo extract
                dismiss() // Close the dialog
            }
        }

        binding.dcdCancel.setOnClickListener{ dismiss() } // Close the dialog

        return binding.root
    }
}