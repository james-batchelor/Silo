package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.*
import com.silofinance.silo.accounts.AccountsViewModel
import com.silofinance.silo.accounts.AccountsViewModelFactory
import com.silofinance.silo.databinding.DialogCreateAccountBinding
import com.silofinance.silo.db.Account
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class CreateAccountDialog : DialogFragment() {

    companion object{
        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance() : CreateAccountDialog {
            return CreateAccountDialog()
        }
    }

    private lateinit var binding: DialogCreateAccountBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_create_account, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Restricts amount inputs to 2dp (and up to order E+8)
        binding.dcaAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        // Restricts emoji inputs to emoji's
        binding.dcaEmoji.filters = arrayOf<InputFilter>(EmojiInputFilter(requireContext()))
        binding.dcaEmoji.addTextChangedListener(getEmojiTextWatcher(binding.dcaEmoji))

        // Tries to create an account. If successful, closes the dialog
        binding.dcaOk.setOnClickListener {
            hideKeyboard()

            if (binding.dcaEmoji.text.isBlank()) {
                Toast.makeText(context, "Enter an emoji", Toast.LENGTH_SHORT).show() //todo extract
            } else if (binding.dcaName.text.isBlank()) {
                Toast.makeText(context, "Enter a name", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                val newAccount = Account()
                newAccount.aEmoji = binding.dcaEmoji.text.toString()
                newAccount.aName = binding.dcaName.text.toString()
                newAccount.aCleared = when (binding.dcaAmount.text.toString()) {
                    "" -> 0.0
                    else -> binding.dcaAmount.text.toString().toDouble()
                }

                viewModel.insertAccount(newAccount)
                Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()  // Close the dialog
            }
        }

        // Help message onClickListeners. Each creates the appropriate CreateAccountHelpDialog
        binding.dcaCredit.setOnClickListener{
            hideKeyboard()
            CreateAccountHelpDialog.newInstance("CREDIT").show(childFragmentManager, "CreateAccountHelpDialogTag")
        }
        binding.dcaOverdrawn.setOnClickListener{
            hideKeyboard()
            CreateAccountHelpDialog.newInstance("OVERDRAWN").show(childFragmentManager, "CreateAccountHelpDialogTag")
        }
        binding.dcaLoan.setOnClickListener{
            hideKeyboard()
            CreateAccountHelpDialog.newInstance("LOAN").show(childFragmentManager, "CreateAccountHelpDialogTag")
        }

        binding.dcaCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }
}