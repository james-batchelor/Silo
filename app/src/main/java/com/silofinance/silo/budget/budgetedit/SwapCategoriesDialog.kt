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
import com.silofinance.silo.databinding.DialogSwapCategoriesBinding
import com.silofinance.silo.db.*


class SwapCategoriesDialog : DialogFragment() {

    companion object{
        private const val ARG_FROMCID = "swapCategoriesDialog_fromCId"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(cId: Long) : SwapCategoriesDialog {
            val args = Bundle()
            args.putLong(ARG_FROMCID, cId)
            val fragment = SwapCategoriesDialog()
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
        val binding: DialogSwapCategoriesBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_swap_categories, container, false)
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
            val categorySpinnerAdapter = CategorySpinnerAdapter(requireContext(), it)
            binding.dscCategory.adapter = categorySpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (category in it) {
                if (category.cId == fromCId) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.dscCategory.setSelection(i)
        } })

        // Checks if the selected category is different, then merges and closes the dialog
        binding.dscOk.setOnClickListener{
            val swapWithCategory = binding.dscCategory.selectedItem as Category
            if (swapWithCategory.cId == fromCId) {
                Toast.makeText(context, "Can't swap with the same category", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                viewModel.categoriesSwapOrder(fromCId, swapWithCategory.cId)
                dismiss()  // Close the dialog
            }
        }

        binding.dscCancel.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}