package com.silofinance.silo.budget.budgetedit

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
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.DialogEditEmojiNameCategoryBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb
import com.silofinance.silo.getEmojiTextWatcher
import com.silofinance.silo.hideKeyboard


class EditEmojiNameCategoryDialog : DialogFragment() {

    companion object{
        private const val ARG_CID = "editEmojiNameCategoryDialog_cId"
        private const val ARG_OLDEMOJI = "editEmojiNameCategoryDialog_oldEmoji"
        private const val ARG_OLDNAME = "editEmojiNameCategoryDialog_oldName"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(cId: Long, oldEmoji: String, oldName: String) : EditEmojiNameCategoryDialog {
            val args = Bundle()
            args.putLong(ARG_CID, cId)
            args.putString(ARG_OLDEMOJI, oldEmoji)
            args.putString(ARG_OLDNAME, oldName)
            val fragment = EditEmojiNameCategoryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var cId: Long = 0L
    private var oldEmoji: String = ""
    private var oldName: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            cId = requireArguments().getLong(ARG_CID)
            oldEmoji = requireArguments().getString(ARG_OLDEMOJI).toString()
            oldName = requireArguments().getString(ARG_OLDNAME).toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogEditEmojiNameCategoryBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit_emoji_name_category, container, false)

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        binding.deencEmoji.setText(oldEmoji)
        binding.deencEmoji.filters = arrayOf<InputFilter>(EditEmojiInputFilter(requireContext(), oldEmoji))
        binding.deencEmoji.addTextChangedListener(getEmojiTextWatcher(binding.deencEmoji))

        binding.deencName.setText(oldName)

        binding.deencSave.setOnClickListener{
            hideKeyboard()

            if (binding.deencEmoji.text.isBlank()) {
                Toast.makeText(context, "Enter an emoji", Toast.LENGTH_SHORT).show() //todo extract
            } else if (binding.deencName.text.isBlank()) {
                Toast.makeText(context, "Enter a name", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                val emoji = binding.deencEmoji.text.toString()
                val name = binding.deencName.text.toString()
                viewModel.updateCategoryEmojiName(cId, emoji, name)
                dismiss()
            }
        }

        binding.deencCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }
}