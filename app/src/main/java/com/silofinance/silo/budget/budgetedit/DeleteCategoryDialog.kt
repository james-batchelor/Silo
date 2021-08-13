package com.silofinance.silo.budget.budgetedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.DialogDeleteCategoryBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class DeleteCategoryDialog : DialogFragment() {

    companion object{
        private const val ARG_CID = "deleteCategoryDialog_cId"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(cId: Long) : DeleteCategoryDialog {
            val args = Bundle()
            args.putLong(ARG_CID, cId)
            val fragment = DeleteCategoryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var cId: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            cId = requireArguments().getLong(ARG_CID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogDeleteCategoryBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_delete_category, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Deletes the category and closes the dialog
        binding.ddcDelete.setOnClickListener{
            viewModel.deleteCategory(cId)
            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()  // todo extract
            dismiss() // Close the dialog
        }

        binding.ddcCancel.setOnClickListener{ dismiss() } // Close the dialog

        return binding.root
    }
}