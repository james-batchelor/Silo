package com.silofinance.silo.budget

import android.content.Context
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
import com.silofinance.silo.adaptersandutils.CategorySpinnerAdapter
import com.silofinance.silo.databinding.DialogMoveMoneyBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.Category
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class MoveMoneyDialog : DialogFragment() {

    companion object{
        private const val ARG_FROMCID = "moveMoneyDialog_fromCId"
        private const val ARG_BUDGET_FRAGMENT = "editIncomeDialog_budget_fragment"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(fromCId: Long, budgetFragment: BudgetFragment) : MoveMoneyDialog {
            val args = Bundle()
            args.putLong(ARG_FROMCID, fromCId)
            args.putParcelable(ARG_BUDGET_FRAGMENT, budgetFragment)
            val fragment = MoveMoneyDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var fromCId: Long = 0L
    private lateinit var budgetFragment: BudgetFragment
    private lateinit var binding: DialogMoveMoneyBinding
    private lateinit var viewModel: BudgetViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            fromCId = requireArguments().getLong(ARG_FROMCID)
            budgetFragment = requireArguments().getParcelable(ARG_BUDGET_FRAGMENT)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_move_money, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        if (fromCId == -4L) {
            binding.dmmText.visibility = View.VISIBLE
        }

        // Restricts amount inputs to 2dp (and up to order E+8)
        binding.dmmAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        // Category spinner
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let { activeCategories ->
            val tbbCategory = Category(cId = -4)
            tbbCategory.cType = 4  // tbb
            tbbCategory.cEmoji = "📤" // Placeholder, the adapter should detect cType == 4 and set this to invisible
            tbbCategory.cName = "To Be Budgeted"
            val list = mutableListOf(tbbCategory)
            activeCategories.forEach { list.add(it) }

            val categorySpinnerAdapter = CategorySpinnerAdapter(requireContext(), list)
            binding.dmmCategoryTo.adapter = categorySpinnerAdapter
        } })

        // Tries to move the money. If successful, the dialog is closed
        binding.dmmMove.setOnClickListener{
            hideKeyboard()
            if (moveMoney()) {
                dismiss()  // Close the dialog
            }
        }

        // Tries to move the money. If successful, the amount field is cleared, the help text is made gone, and a toast declares the money was moved
        binding.dmmMove.setOnLongClickListener{
            hideKeyboard()
            if (moveMoney()) {
                binding.dmmAmount.text.clear()
                binding.dmmText.visibility = View.GONE
                Toast.makeText(context, "Money moved", Toast.LENGTH_SHORT).show() //todo extract
            }
            true  // Notifies the phone that the long click was consumed
        }

        binding.dmmCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Checks if a different category was selected, then moves the money between the categories, and updates the tbb bar in BudgetFragment */
    private fun moveMoney() : Boolean {
        val toCategory = binding.dmmCategoryTo.selectedItem as Category
        if (toCategory.cId == fromCId) {
            Toast.makeText(context, "Can't move to the same category", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        val amount = when(binding.dmmAmount.text.toString()) {
            "" -> 0.0
            else -> binding.dmmAmount.text.toString().toDouble()
        }
        viewModel.moveTheMoney(fromCId, toCategory, amount)

        // Refreshes tbb. Do this here rather than in viewModel.moveTheMoney so that we know budgetFragment.setUpTbb() will be called AFTER the tbb is actually changed
        if (fromCId == -4L || toCategory.cId == -4L) {  // So if we are moving from/to tbb
            val tbbSharedPref = requireContext().getSharedPreferences(requireContext().getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
            var tbb = tbbSharedPref.getDouble("tbb", 0.0)
            tbb = when {
                fromCId == -4L -> tbb - amount
                toCategory.cId == -4L -> tbb + amount
                else -> tbb
            }
            tbbSharedPref.edit().putDouble("tbb", tbb).apply()
            budgetFragment.setUpTbb()
        }

        return true
    }
}