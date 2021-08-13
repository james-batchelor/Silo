package com.silofinance.silo.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.databinding.DialogDeleteTransactionBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.db.TransactionDb


class DeleteTransactionDialog : DialogFragment() {

    companion object{
        private const val ARG_TRANSACTION = "deleteTransactionDialog_transaction"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(transaction: Transaction) : DeleteTransactionDialog {
            val args = Bundle()
            args.putParcelable(ARG_TRANSACTION, transaction)
            val fragment = DeleteTransactionDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var transaction: Transaction


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            transaction = requireArguments().getParcelable(ARG_TRANSACTION)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogDeleteTransactionBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_delete_transaction, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = TransactionsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(TransactionsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        when (transaction.tType) {
            1 -> {
                binding.ddtTitle.text = "Delete Income"  // todo extract
                binding.ddtText.text = "Are you sure you want to delete this income?" }  // todo extract
            2 -> {
                binding.ddtTitle.text = "Delete Expense"  // todo extract
                binding.ddtText.text = "Are you sure you want to delete this expense?" }  // todo extract
            3 -> {
                binding.ddtTitle.text = "Delete Transfer"  // todo extract
                binding.ddtText.text = "Are you sure you want to delete this transfer?" }  // todo extract
        }

        // Deletes the transaction, toasts what type was deleted, and closes the dialog
        binding.ddtDelete.setOnClickListener{
            when (transaction.tType) {
                1 -> { viewModel.deleteIncome(transaction.tId)
                    Toast.makeText(context, "Income deleted", Toast.LENGTH_SHORT).show() }  // todo extract
                2 -> { viewModel.deleteExpense(transaction.tId)
                    Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show() }  // todo extract
                3 -> { viewModel.deleteTransfer(transaction.tId)
                    Toast.makeText(context, "Transfer deleted", Toast.LENGTH_SHORT).show() }  // todo extract
            }
            dismiss() // Close the dialog
        }

        binding.ddtCancel.setOnClickListener{ dismiss() } // Close the dialog

        return binding.root
    }
}