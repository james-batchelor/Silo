package com.silofinance.silo.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.silofinance.silo.DecimalDigitsInputFilter
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.AccountSpinnerAdapter
import com.silofinance.silo.adaptersandutils.CategorySpinnerAdapter
import com.silofinance.silo.databinding.DialogEditExpenseBinding
import com.silofinance.silo.db.*
import com.silofinance.silo.hideKeyboard
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class EditExpenseDialog : DialogFragment() {

    companion object{
        private const val ARG_EXPENSE = "editExpenseDialog_expense"
        private const val ARG_TRANSACTIONS_FRAGMENT = "editIncomeDialog_transactionsFragment"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(expense: Transaction, transactionsFragment: TransactionsFragment) : EditExpenseDialog {
            val args = Bundle()
            args.putParcelable(ARG_EXPENSE, expense)
            args.putParcelable(ARG_TRANSACTIONS_FRAGMENT, transactionsFragment)
            val fragment = EditExpenseDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var expense: Transaction
    private lateinit var transactionsFragment: TransactionsFragment
    private lateinit var binding: DialogEditExpenseBinding
    private lateinit var cal: Calendar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            expense = requireArguments().getParcelable(ARG_EXPENSE)!!
            transactionsFragment = requireArguments().getParcelable(ARG_TRANSACTIONS_FRAGMENT)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit_expense, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = TransactionsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(TransactionsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Restricts amount inputs to 2dp (and up to order E+8)
        binding.deeAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        // Account spinner
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            if (it.isEmpty()) {
                Toast.makeText(context, "No active accounts to edit to", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()
            }
            val accountSpinnerAdapter = AccountSpinnerAdapter(requireContext(), it)
            binding.deeAccount.adapter = accountSpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (account in it) {
                if (account.aId == expense.tAccount) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.deeAccount.setSelection(i)
        } })

        // Category spinner
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let {
            if (it.isEmpty()) {
                Toast.makeText(context, "No active categories to edit to", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()
            }
            val categorySpinnerAdapter = CategorySpinnerAdapter(requireContext(), it)
            binding.deeCategory.adapter = categorySpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (category in it) {
                if (category.cId == expense.tCategory) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.deeCategory.setSelection(i)
        } })

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date
        val localDate = LocalDate.ofEpochDay(expense.tDate)
        cal.time = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())  // Set the date of cal to expense.tDate

        // Edits the expense and closes the dialog
        binding.deeSave.setOnClickListener {
            hideKeyboard()

            val newExpense = Transaction(tId = expense.tId)  // They share a tId and so DAO.insert(newIncome) overwrites income
            newExpense.tType = 2  // 2 for expense
            newExpense.tDate = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalDate().toEpochDay()
            val account = binding.deeAccount.selectedItem as Account
            newExpense.tAccount = account.aId
            val category = binding.deeCategory.selectedItem as Category
            newExpense.tCategory = category.cId
            newExpense.tAmount = when (binding.deeAmount.text.toString()) {
                "" -> 0.0
                else -> -binding.deeAmount.text.toString().toDouble()
            }
            newExpense.tNote = binding.deeNote.text.toString()
            newExpense.tCleared = binding.deeCleared.isChecked

            viewModel.editExpense(expense, newExpense)
            dismiss()  // Close the dialog
        }

        // Opens the delete transaction (expense) dialog
        binding.deeDelete.setOnClickListener {
            hideKeyboard()
            transactionsFragment.openDeleteDialog(expense)
            dismiss()  // Close the dialog
        }

        binding.deeCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Initialise views and set up the onClickListeners */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.deeCleared.isChecked = expense.tCleared
        var amountString = when (expense.tAmount >= -0.005) {
            true -> expense.tAmount.toString()
            else -> (-expense.tAmount).toString()  // Mainly to catch 0.0 (or -0.000521..) from displaying -0.00
        }
        if (amountString[amountString.lastIndex - 1] == "."[0]) {  // So if amountString ends like x.x instead of x.xx
            amountString = "${amountString}0"  // Append a 0
        }
        binding.deeAmount.setText(amountString)
        binding.deeNote.setText(expense.tNote)

        // Get the stuff used to choose the date ready
        val settingsSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val settingsDate = settingsSharedPrefs.getString("settings_date", "")
        val dateFormat = when (settingsDate) {  // https://developer.android.com/reference/java/text/SimpleDateFormat If using non Locale.ENGLISH locales later, make sure that special characters don't mess things up, from accents to chinese characters
            "dmySlash" -> SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
            "dmyDot" -> SimpleDateFormat("dd.MM.yy", Locale.ENGLISH)
            "mdySlash" -> SimpleDateFormat("MM/dd/yy", Locale.ENGLISH)
            "mdyDot" -> SimpleDateFormat("MM.dd.yy", Locale.ENGLISH)
            "ymdSlash" -> SimpleDateFormat("yy/MM/dd", Locale.ENGLISH)
            "ymdDot" -> SimpleDateFormat("yy.MM.dd", Locale.ENGLISH)
            else -> SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
        }
        val datePickerDialog = DatePickerDialog(  // This date picker dialog is opened in the onClickListener for the date button below
            requireNotNull(activity), { _, year, month, dayOfMonth ->  // This second argument is a DatePickerDialog.OnDateSetListener. So, update the "cal" Calendar instance, and the text of the button, with the chosen date
                cal.set(year, month, dayOfMonth)
                binding.deeDate.text = dateFormat.format(cal.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)  // Needed to open the datePicker to the date of cal (initially the current date, then the chosen date)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()  // Disallow choosing future dates

        // Give the date button the initial date, and set its onClickListener to open the datePicker dialog (which updates this same button itself)
        binding.deeDate.text = dateFormat.format(cal.time)
        binding.deeDate.setOnClickListener { datePickerDialog.show() }
    }
}