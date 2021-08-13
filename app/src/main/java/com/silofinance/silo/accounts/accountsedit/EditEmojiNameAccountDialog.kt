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
import com.silofinance.silo.EditEmojiInputFilter
import com.silofinance.silo.R
import com.silofinance.silo.accounts.AccountsViewModel
import com.silofinance.silo.accounts.AccountsViewModelFactory
import com.silofinance.silo.databinding.DialogEditEmojiNameAccountBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb
import com.silofinance.silo.getEmojiTextWatcher
import com.silofinance.silo.hideKeyboard


class EditEmojiNameAccountDialog : DialogFragment() {

    companion object{
        private const val ARG_AID = "editEmojiNameAccountDialog_aId"
        private const val ARG_OLDEMOJI = "editEmojiNameAccountDialog_oldEmoji"
        private const val ARG_OLDNAME = "editEmojiNameAccountDialog_oldName"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(aId: Long, oldEmoji: String, oldName: String) : EditEmojiNameAccountDialog {
            val args = Bundle()
            args.putLong(ARG_AID, aId)
            args.putString(ARG_OLDEMOJI, oldEmoji)
            args.putString(ARG_OLDNAME, oldName)
            val fragment = EditEmojiNameAccountDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var aId: Long = 0L
    private var oldEmoji: String = ""
    private var oldName: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            aId = requireArguments().getLong(ARG_AID)
            oldEmoji = requireArguments().getString(ARG_OLDEMOJI).toString()
            oldName = requireArguments().getString(ARG_OLDNAME).toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogEditEmojiNameAccountBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit_emoji_name_account, container, false)

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        binding.deenaEmoji.setText(oldEmoji)
        binding.deenaEmoji.filters = arrayOf<InputFilter>(EditEmojiInputFilter(requireContext(), oldEmoji))
        binding.deenaEmoji.addTextChangedListener(getEmojiTextWatcher(binding.deenaEmoji))

        binding.deenaName.setText(oldName)

        binding.deenaSave.setOnClickListener{
            hideKeyboard()

            if (binding.deenaEmoji.text.isBlank()) {
                Toast.makeText(context, "Enter an emoji", Toast.LENGTH_SHORT).show() //todo extract
            } else if (binding.deenaName.text.isBlank()) {
                Toast.makeText(context, "Enter a name", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                val emoji = binding.deenaEmoji.text.toString()
                val name = binding.deenaName.text.toString()
                viewModel.updateAccountEmojiName(aId, emoji, name)
                dismiss()
            }
        }

        binding.deenaCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }
}