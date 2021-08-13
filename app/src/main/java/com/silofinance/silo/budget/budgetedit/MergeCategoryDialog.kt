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
import com.silofinance.silo.adaptersandutils.CategorySpinnerAdapter
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.DialogMergeCategoryBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.Category
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class MergeCategoryDialog : DialogFragment() {

    companion object{
        private const val ARG_FROMCID = "mergeCategoryDialog_fromCId"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(fromCId: Long) : MergeCategoryDialog {
            val args = Bundle()
            args.putLong(ARG_FROMCID, fromCId)
            val fragment = MergeCategoryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var fromCId: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            fromCId = requireArguments().getLong(ARG_FROMCID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogMergeCategoryBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_merge_category, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Category spinner
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let {
            if (it.isEmpty()) {
                Toast.makeText(context, "No active categories to merge to", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()
            }
            val categorySpinnerAdapter = CategorySpinnerAdapter(requireContext(), it)
            binding.dmcCategory.adapter = categorySpinnerAdapter
        } })

        // Checks if the selected category is different, then merges and closes the dialog
        binding.dmcMerge.setOnClickListener{
            val toCategory = binding.dmcCategory.selectedItem as Category
            if (toCategory.cId == fromCId) {
                Toast.makeText(context, "Can't merge to the same category", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                viewModel.mergeCategory(fromCId, toCategory.cId)
                Toast.makeText(context, "Category merged", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()  // Close the dialog
            }
        }

        binding.dmcCancel.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}