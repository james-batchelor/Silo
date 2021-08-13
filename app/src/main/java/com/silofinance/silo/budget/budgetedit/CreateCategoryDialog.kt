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
import com.silofinance.silo.EmojiInputFilter
import com.silofinance.silo.R
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.DialogCreateCategoryBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.Category
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb
import com.silofinance.silo.getEmojiTextWatcher
import com.silofinance.silo.hideKeyboard


class CreateCategoryDialog : DialogFragment() {

    companion object{
        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance() : CreateCategoryDialog {
            return CreateCategoryDialog()
        }
    }

    private lateinit var binding: DialogCreateCategoryBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_create_category, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Restricts emoji inputs to emoji's
        binding.dccEmoji.filters = arrayOf<InputFilter>(EmojiInputFilter(requireContext()))
        binding.dccEmoji.addTextChangedListener(getEmojiTextWatcher(binding.dccEmoji))

        // Tries to create a category. If successful, closes the dialog
        binding.dccOk.setOnClickListener {
            hideKeyboard()
            if (requiredFieldsAreValid()) {
                val newCategory = Category()
                newCategory.cEmoji = binding.dccEmoji.text.toString()
                newCategory.cName = binding.dccName.text.toString()

                viewModel.insertCategory(newCategory)
                Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()  // Close the dialog
            }
        }

        binding.dccHelp.setOnClickListener{
            hideKeyboard()
            CreateCategoryHelpDialog.newInstance().show(childFragmentManager, "CreateCategoryHelpDialogTag")
        }

        binding.dccCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Checks if an emoji and a name were entered */
    private fun requiredFieldsAreValid() : Boolean {
        if (binding.dccEmoji.text.isBlank()) {
            Toast.makeText(context, "Enter an emoji", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        if (binding.dccName.text.isBlank()) {
            Toast.makeText(context, "Enter a name", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        return true
    }
}