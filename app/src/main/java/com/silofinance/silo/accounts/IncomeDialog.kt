package com.silofinance.silo.accounts

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
import com.silofinance.silo.databinding.DialogIncomeBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.db.TransactionDb
import com.silofinance.silo.hideKeyboard
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class IncomeDialog : DialogFragment() {

    companion object{
        private const val ARG_AID = "incomeDialog_aId"
        private const val ARG_AAUTOCLEAR = "incomeDialog_aAutoclear"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(aId: Long, aAutoclear: Boolean) : IncomeDialog {
            val args = Bundle()
            args.putLong(ARG_AID, aId)
            args.putBoolean(ARG_AAUTOCLEAR, aAutoclear)
            val fragment = IncomeDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var aId: Long = 0L
    private var aAutoclear: Boolean = false
    private lateinit var binding: DialogIncomeBinding
    private lateinit var viewModel: AccountsViewModel
    private lateinit var cal: Calendar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            aId = requireArguments().getLong(ARG_AID)
            aAutoclear = requireArguments().getBoolean(ARG_AAUTOCLEAR)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_income, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Restricts amount inputs to 2dp (and up to order E+8)
        binding.diAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date

        binding.diCleared.isChecked = aAutoclear  // Apply autoclear

        // Tries to add an income. If successful, closes the dialog
        binding.diOk.setOnClickListener{
            hideKeyboard()
            if (makeIncome()) {
                dismiss()  // Close the dialog
            }
        }

        // Tries to add an income. If successful then the amount/note fields are cleared and a toast declares the income was added (the dialog stays open)
        binding.diOk.setOnLongClickListener{
            hideKeyboard()
            if (makeIncome()) {
                binding.diAmount.text.clear()
                binding.diNote.text.clear()
                Toast.makeText(context, "Income added", Toast.LENGTH_SHORT).show() //todo extract
            }
            true  // Notifies the phone that the long click was consumed
        }

        binding.diCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Makes an income and adds it to the TransactionDb */
    private fun makeIncome() : Boolean {
        if (binding.diAmount.text.isBlank()) {
            Toast.makeText(context, "Enter an amount", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        val newIncome = Transaction()
        newIncome.tType = 1  // 1 for income
        newIncome.tDate = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalDate().toEpochDay()
        newIncome.tAccount = aId  // The account tapped to open this dialog
        newIncome.tCategory = -1  // -1 for income
        newIncome.tAmount = binding.diAmount.text.toString().toDouble()
        newIncome.tNote = binding.diNote.text.toString()
        newIncome.tCleared = binding.diCleared.isChecked

        viewModel.insertIncome(newIncome)
        return true
    }

    /** Initialise views and set up the onClickListeners */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                binding.diDate.text = dateFormat.format(cal.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)  // Needed to open the datePicker to the date of cal (initially the current date, then the chosen date)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()  // Disallow choosing future dates

        // Give the date button the initial date, and set its onClickListener to open the datePicker dialog (which updates this same button itself)
        binding.diDate.text = dateFormat.format(cal.time)
        binding.diDate.setOnClickListener { datePickerDialog.show() }
    }
}