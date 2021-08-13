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
import com.silofinance.silo.adaptersandutils.AccountSpinnerAdapter
import com.silofinance.silo.databinding.DialogSwapAccountsBinding
import com.silofinance.silo.db.Account
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class SwapAccountsDialog : DialogFragment() {

    companion object{
        private const val ARG_FROMAID = "swapAccountsDialog_fromAId"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(aId: Long) : SwapAccountsDialog {
            val args = Bundle()
            args.putLong(ARG_FROMAID, aId)
            val fragment = SwapAccountsDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var fromAId: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            fromAId = requireArguments().getLong(ARG_FROMAID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogSwapAccountsBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_swap_accounts, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Account spinner
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            val accountSpinnerAdapter = AccountSpinnerAdapter(requireContext(), it)
            binding.dsaAccount.adapter = accountSpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (account in it) {
                if (account.aId == fromAId) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.dsaAccount.setSelection(i)
        } })

        // Checks if the selected account is different, then merges and closes the dialog
        binding.dsaOk.setOnClickListener{
            val swapWithAccount = binding.dsaAccount.selectedItem as Account
            if (swapWithAccount.aId == fromAId) {
                Toast.makeText(context, "Can't swap with the same account", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                viewModel.accountsSwapOrder(fromAId, swapWithAccount.aId)
                dismiss()  // Close the dialog
            }
        }

        binding.dsaCancel.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}