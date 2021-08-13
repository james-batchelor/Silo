package com.silofinance.silo.expense

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.silofinance.silo.DecimalDigitsInputFilter
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.AccountEmojiAdapter
import com.silofinance.silo.adaptersandutils.CategoryEmojiAdapter
import com.silofinance.silo.databinding.FragmentExpenseBinding
import com.silofinance.silo.db.*
import com.silofinance.silo.hideKeyboard
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class ExpenseFragment : Fragment() {

    private lateinit var binding: FragmentExpenseBinding
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var cal: Calendar
    private lateinit var activeAccountsList: List<Account>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_expense, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = ExpenseViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        viewModel = ViewModelProvider(this, viewModelFactory).get(ExpenseViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        viewModel.ensureSelections()  // If no account or category is selected, this selects the highest order one

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date

        // Restricts amount inputs to 2dp (and up to order E+8)
        binding.feAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())

        // Account emoji RecyclerView stuff
        val accountEmojiAdapter = AccountEmojiAdapter { aId ->  // selectAccountClickListener
            viewModel.selectAccount(aId)
        }
        binding.feAccount.adapter = accountEmojiAdapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            accountEmojiAdapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
            binding.feAccountLabel.visibility = View.VISIBLE  // These start out as invisible so that the ui doesn't jump around while the recyclers load
            binding.feAccCatDivider.visibility = View.VISIBLE
            activeAccountsList = it  // Get this so that autoclear can be applied in selectAccountClickListener

            // Apply the autoclear
            for (account in it) {
                if (account.aSelected) {
                    binding.feCleared.isChecked = account.aAutoclear
                    break
                }
            }
        } })
        val accountRecyclerManager = GridLayoutManager(activity, 7)
        binding.feAccount.layoutManager = accountRecyclerManager  // Use a grid to display the list_item's

        // Category emoji RecyclerView stuff
        val categoryEmojiAdapter = CategoryEmojiAdapter { cId ->  // selectCategoryClickListener
            viewModel.selectCategory(cId)
        }
        binding.feCategory.adapter = categoryEmojiAdapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let {
            categoryEmojiAdapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
            binding.feCategoryLabel.visibility = View.VISIBLE  // These start out as invisible so that the ui doesn't jump around while the recyclers load
            binding.feCatAmountDivider.visibility = View.VISIBLE
        } })
        val categoryRecyclerManager = GridLayoutManager(activity, 7)
        binding.feCategory.layoutManager = categoryRecyclerManager  // Use a grid to display the list_item's

        cal = Calendar.getInstance()  // Creates a Calendar instance, automatically set to the current date

        // Tries to add an expense. If successful, backs out of the fragment
        binding.feOk.setOnClickListener {
            if (makeExpense()) {
                findNavController().popBackStack()  // Press back
            }
        }

        // Tries to add an expense. If successful, clears the amount/note fields and a toast declares the expense was added
        binding.feOk.setOnLongClickListener{
            if (makeExpense()) {
                binding.feAmount.text.clear()
                binding.feNote.text.clear()
                Toast.makeText(context, "Expense added", Toast.LENGTH_SHORT).show() //todo extract
            }
            true  // Notifies the phone that the long click was consumed
        }

        binding.feCancel.setOnClickListener{
            hideKeyboard()
            findNavController().popBackStack()  // Press back
        }

        return binding.root
    }

    /** Checks if an amount was entered, then makes an expense and adds it to the TransactionDb */
    private fun makeExpense() : Boolean {
        hideKeyboard()

        if (binding.feAmount.text.isBlank()) {
            Toast.makeText(context, "Enter an amount", Toast.LENGTH_SHORT).show() //todo extract
            return false
        }

        val newExpense = Transaction()
        newExpense.tType = 2  // 2 for expense
        newExpense.tDate = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalDate().toEpochDay()
        newExpense.tAmount = -(binding.feAmount.text.toString().toDouble())
        newExpense.tNote = binding.feNote.text.toString()
        newExpense.tCleared = binding.feCleared.isChecked

        viewModel.insertExpense(newExpense)
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
                binding.feDate.text = dateFormat.format(cal.time)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)  // Needed to open the datePicker to the date of cal (initially the current date, then the chosen date)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()  // Disallow choosing future dates

        // Give the date button the initial date, and set its onClickListener to open the datePicker dialog (which updates this same button itself)
        binding.feDate.text = dateFormat.format(cal.time)
        binding.feDate.setOnClickListener { datePickerDialog.show() }
    }
}