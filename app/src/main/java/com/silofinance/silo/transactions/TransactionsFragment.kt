package com.silofinance.silo.transactions

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.TransactionAdapter
import com.silofinance.silo.databinding.FragmentTransactionsBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.db.TransactionDb
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize


@Parcelize  // So that we can pass this fragment ("this" in the EditDialog.newInstance functions below) into the edit dialogs, so that they can call openDeleteDialog()
class TransactionsFragment : Fragment(), Parcelable {

    @IgnoredOnParcel
    private lateinit var hiddenAccountIdsList: List<Long>
    @IgnoredOnParcel
    private lateinit var hiddenCategoryIdsList: List<Long>
    @IgnoredOnParcel
    private var highestActiveAId = 0L
    @IgnoredOnParcel
    private var highestActiveCId = 0L


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentTransactionsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_transactions, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = TransactionsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(TransactionsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Get references to the List<Long>>'s that hold the Id's of the hidden accounts and categories respectively (to block editing)
        viewModel.hiddenAccountIds.observe(viewLifecycleOwner, { it?.let { hiddenAccountIdsList = it } })
        viewModel.hiddenCategoryIds.observe(viewLifecycleOwner, { it?.let { hiddenCategoryIdsList = it } })

        // Set the highestId's (used in navigateToNewExpense() to check that ANY accounts/categories exist)
        viewModel.highestIdActiveAccount.observe(viewLifecycleOwner, { highestActiveAId = it?: 0L })
        viewModel.highestIdActiveCategory.observe(viewLifecycleOwner, { highestActiveCId = it?: 0L })

        // RecyclerView stuff
        val adapter = TransactionAdapter({ transaction ->  // flipClearedClickListener
            viewModel.flipCleared(transaction.tId)
        }, { transaction ->  // editLongClickListener
            if (hiddenCheck(transaction)) {  // See end of file
                val editTransactionDialog = when (transaction.tType) {
                    1 -> EditIncomeDialog.newInstance(transaction, this)
                    2 -> EditExpenseDialog.newInstance(transaction, this)
                    3 -> EditTransferDialog.newInstance(transaction, this)
                    else -> throw error("Tried to edit a transaction with a tType not 1, 2 or 3")
                }
                editTransactionDialog.show(childFragmentManager, "EditTransactionDialogTag")
            }
        })
        binding.ftRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.allTransactions.observe(viewLifecycleOwner, { it?.let {
            adapter.submitList(it)
            binding.ftRecycler.visibility = View.VISIBLE  // Draw the recycler only after it has loaded its list so that the layoutAnimation runs reliably
        } })

        // Toolbar and fab
        //setHasOptionsMenu(true)  // Essentially causes a call to onCreateOptionsMenu, which creates the options menu in the toolbar
        viewModel.navigateToAddExpense.observe(viewLifecycleOwner, { if(it) {
            navigateToNewExpense()
            viewModel.onNavigatedToNewt()  // Reset the observed navigateToAddExpense LiveData<Boolean>
        } })
        binding.ftRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {  // Hide the FAB when scrolling down, show it when scrolling up
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.ftFab.visibility == View.VISIBLE) {
                    binding.ftFab.hide()
                } else if (dy < 0 && binding.ftFab.visibility != View.VISIBLE) {
                    binding.ftFab.show()
                }
            }
        })

        return binding.root
    }

    /** Opens the delete transaction dialog for the given transaction */
    fun openDeleteDialog(transaction: Transaction) {
        val deleteTransactionDialog = DeleteTransactionDialog.newInstance(transaction)
        deleteTransactionDialog.show(childFragmentManager, "DeleteTransactionDialogTag")
    }

    /** Checks if both the tAccount and tCategory are active objects. Returns the result, making a toast if it failed */
    private fun hiddenCheck(transaction: Transaction) : Boolean {
        if (transaction.tType != 3) {
            if (transaction.tCategory in hiddenCategoryIdsList) {
                Toast.makeText(context, "Can't edit (category is hidden)", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (transaction.tType == 3) {
            if (transaction.tCategory in hiddenAccountIdsList) {
                Toast.makeText(context, "Can't edit (account is hidden)", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (transaction.tAccount in hiddenAccountIdsList) {
            Toast.makeText(context, "Can't edit (account is hidden)", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    /** Checks if accounts and categories exist. If they don't, display a toast. If they do, navigate to new expense */
    private fun navigateToNewExpense(){
        if (highestActiveAId == 0L) {
            if (highestActiveCId == 0L) {
                Toast.makeText(context, "Add some accounts and categories first", Toast.LENGTH_SHORT).show()
                return
            }
            Toast.makeText(context, "Add some accounts first", Toast.LENGTH_SHORT).show()
            return
        }
        if (highestActiveCId == 0L) {
            Toast.makeText(context, "Add some categories first", Toast.LENGTH_SHORT).show()
            return
        }
        val navController = findNavController()
        navController.navigate(R.id.action_transactionsFragment_to_expenseFragment)
    }

//    /** Inflate the (toolbar) menu */
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.menu_transactions, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }

//    /** Handle menu item selection */
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.menu_transactions_filter -> {
//                Toast.makeText(context, "Filter", Toast.LENGTH_SHORT).show()  //todo extract
//                true
//            }
//            R.id.menu_transactions_sort -> {
//                Toast.makeText(context, "Sort", Toast.LENGTH_SHORT).show()  //todo extract
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
}

