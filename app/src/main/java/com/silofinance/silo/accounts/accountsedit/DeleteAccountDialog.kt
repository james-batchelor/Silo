package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.accounts.AccountsViewModel
import com.silofinance.silo.accounts.AccountsViewModelFactory
import com.silofinance.silo.databinding.DialogDeleteAccountBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class DeleteAccountDialog : DialogFragment() {

    companion object{
        private const val ARG_AID = "deleteAccountDialog_aId"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(aId: Long) : DeleteAccountDialog {
            val args = Bundle()
            args.putLong(ARG_AID, aId)
            val fragment = DeleteAccountDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var aId: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            aId = requireArguments().getLong(ARG_AID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogDeleteAccountBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_delete_account, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Deletes the account and closes the dialog
        binding.ddaDelete.setOnClickListener{
            viewModel.deleteAccount(aId)
            Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()  // todo extract
            dismiss() // Close the dialog
        }

        binding.ddaCancel.setOnClickListener{ dismiss() } // Close the dialog

        return binding.root
    }
}