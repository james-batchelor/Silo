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
import com.silofinance.silo.databinding.DialogEditTransferBinding
import com.silofinance.silo.db.*
import com.silofinance.silo.hideKeyboard
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class EditTransferDialog : DialogFragment() {

    companion object{
        private const val ARG_TRANSFER = "editTransferDialog_transfer"
        private const val ARG_TRANSACTIONS_FRAGMENT = "editIncomeDialog_transactions_fragment"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(transfer: Transaction, transactionsFragment: TransactionsFragment) : EditTransferDialog {
            val args = Bundle()
            args.putParcelable(ARG_TRANSFER, transfer)
            args.putParcelable(ARG_TRANSACTIONS_FRAGMENT, transactionsFragment)
            val fragment = EditTransferDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var transfer: Transaction
    private lateinit var transactionsFragment: TransactionsFragment
    private lateinit var binding: DialogEditTransferBinding
    private lateinit var cal: Calendar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            transfer = requireArguments().getParcelable(ARG_TRANSFER)!!
            transactionsFragment = requireArguments().getParcelable(ARG_TRANSACTIONS_FRAGMENT)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_edit_transfer, container, false)
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
        binding.detAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        // Account spinners
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            if (it.isEmpty()) {
                Toast.makeText(context, "No active accounts to edit to", Toast.LENGTH_SHORT).show() //todo extract
                dismiss()
            }
            // First, the 'from' account (transfer.tAccount)
            val fromAccountSpinnerAdapter = AccountSpinnerAdapter(requireContext(), it)
            binding.detAccountFrom.adapter = fromAccountSpinnerAdapter
            var i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (fromAccount in it) {
                if (fromAccount.aId == transfer.tAccount) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.detAccountFrom.setSelection(i)

            // Now, the 'to' account (transfer.tCategory)
            val toAccountSpinnerAdapter = AccountSpinnerAdapter(requireContext(), it)
            binding.detAccountTo.adapter = toAccountSpinnerAdapter
            i = 0  // Find the index of the currently selected item, so that the spinner is selecting it when opened
            for (toAccount in it) {
                if (toAccount.aId == transfer.tCategory) {
                    break
                }
                i++
                if (i == it.size) {
                    i = 0
                    break
                }
            }
            binding.detAccountTo.setSelection(i)
        } })

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date
        val localDate = LocalDate.ofEpochDay(transfer.tDate)
        cal.time = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())  // Set the date of cal to expense.tDate

        // If the amount field isn't empty, and the accounts are different, edits the transfer and closes the dialog
        binding.detSave.setOnClickListener {
            hideKeyboard()

            val fromAccount = binding.detAccountFrom.selectedItem as Account
            val toAccount = binding.detAccountTo.selectedItem as Account
            if (fromAccount == toAccount) {
                Toast.makeText(context, "Can't transfer to the same account", Toast.LENGTH_SHORT).show() //todo extract
            } else {
                val newTransfer = Transaction(tId = transfer.tId)  // They share a tId and so DAO.insert(newTransfer) overwrites transfer
                newTransfer.tType = 3  // 3 for transfer
                newTransfer.tDate = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalDate().toEpochDay()
                newTransfer.tAccount = fromAccount.aId
                newTransfer.tCategory = toAccount.aId
                newTransfer.tAmount = when (binding.detAmount.text.toString()) {
                    "" -> 0.0
                    else -> binding.detAmount.text.toString().toDouble()
                }
                newTransfer.tNote = binding.detNote.text.toString()
                newTransfer.tCleared = binding.detCleared.isChecked

                viewModel.editTransfer(transfer, newTransfer)
                dismiss()  // Close the dialog
            }
        }

        // Opens the delete transaction (transfer) dialog
        binding.detDelete.setOnClickListener {
            hideKeyboard()
            transactionsFragment.openDeleteDialog(transfer)
            dismiss()  // Close the dialog
        }

        binding.detCancel.setOnClickListener{
            hideKeyboard()
            dismiss()  // Close the dialog
        }

        return binding.root
    }

    /** Initialise views and set up the onClickListeners */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detCleared.isChecked = transfer.tCleared
        var amountString = transfer.tAmount.toString()
        if (amountString[amountString.lastIndex - 1] == "."[0]) {  // So if amountString ends like x.x instead of x.xx
            amountString = "${amountString}0"  // Append a 0
        }
        binding.detAmount.setText(amountString)
        binding.detNote.setText(transfer.tNote)

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
                binding.detDate.text = dateFormat.format(cal.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)  // Needed to open the datePicker to the date of cal (initially the current date, then the chosen date)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()  // Disallow choosing future dates

        // Give the date button the initial date, and set its onClickListener to open the datePicker dialog (which updates this same button itself)
        binding.detDate.text = dateFormat.format(cal.time)
        binding.detDate.setOnClickListener { datePickerDialog.show() }
    }
}