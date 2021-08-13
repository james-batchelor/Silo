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
import com.silofinance.silo.adaptersandutils.AccountSpinnerAdapter
import com.silofinance.silo.hideKeyboard
import com.silofinance.silo.databinding.DialogTransferBinding
import com.silofinance.silo.db.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class TransferDialog : DialogFragment() {

    companion object{
        private const val ARG_AID = "transferDialog_aId"
        private const val ARG_AAUTOCLEAR = "transferDialog_aAutoclear"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(aId: Long, aAutoclear: Boolean) : TransferDialog {
            val args = Bundle()
            args.putLong(ARG_AID, aId)
            args.putBoolean(ARG_AAUTOCLEAR, aAutoclear)
            val fragment = TransferDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var aId: Long = 0L
    private var aAutoclear: Boolean = false
    private lateinit var binding: DialogTransferBinding
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
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_transfer, container, false)
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
        binding.dtAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date

        binding.dtCleared.isChecked = aAutoclear  // Apply autoclear

        // Account spinner
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            val accountSpinnerAdapter = AccountSpinnerAdapter(requireContext(), it)
            binding.dtAccountTo.adapter = accountSpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (toAccount in it) {
                if (toAccount.aId == aId) {
                    break
                }
                i++
            }
            binding.dtAccountTo.setSelection(i)
        } })

        binding.dtInfo.setOnClickListener {
            OkButtonInfoDialog.newInstance().show(childFragmentManager, "OkButtonInfoDialogTag")
        }

        // Tries to add a transfer. If successful, closes the dialog
        binding.dtOk.setOnClickListener {
            hideKeyboard()
            if (makeTransfer()) {
                dismiss()  // Close the dialog
            }
        }

        // Tries to add a transfer. If successful, the amount/note fields are cleared and a toast declares the transfer was added (the dialog stays open)
        binding.dtOk.setOnLongClickListener{
            hideKeyboard()
            if (makeTransfer()) {
                binding.dtAmount.text.clear()
                binding.dtNote.text.clear()
                Toast.makeText(context, "Transfer added", Toast.LENGTH_SHORT).show() //todo extract
            }
            true  // Notifies the phone that the long click was consumed
        }

        binding.dtCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Checks if an amount and a different account was selected, then makes a transfer and adds it to the TransactionDb */
    private fun makeTransfer() : Boolean {
        val toAccount = binding.dtAccountTo.selectedItem as Account
        if (toAccount.aId == aId) {
            Toast.makeText(context, "Can't transfer to the same account", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }
        if (binding.dtAmount.text.isBlank()) {
            Toast.makeText(context, "Enter an amount", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        val newTransfer = Transaction()
        newTransfer.tType = 3  // 3 for transfer
        newTransfer.tDate = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalDate().toEpochDay()
        newTransfer.tAccount = aId  // The account tapped to open this dialog
        newTransfer.tCategory = toAccount.aId  // The account selected in the spinner
        newTransfer.tAmount = binding.dtAmount.text.toString().toDouble()
        newTransfer.tNote = binding.dtNote.text.toString()
        newTransfer.tCleared = binding.dtCleared.isChecked

        viewModel.insertTransfer(newTransfer)
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
                binding.dtDate.text = dateFormat.format(cal.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)  // Needed to open the datePicker to the date of cal (initially the current date, then the chosen date)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()  // Disallow choosing future dates

        // Give the date button the initial date, and set its onClickListener to open the datePicker dialog (which updates this same button itself)
        binding.dtDate.text = dateFormat.format(cal.time)
        binding.dtDate.setOnClickListener { datePickerDialog.show() }
    }
}