package com.silofinance.silo.accounts

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.AccountAdapter
import com.silofinance.silo.currencyFormat
import com.silofinance.silo.databinding.FragmentAccountsBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class AccountsFragment : Fragment() {

    private var highestActiveAId = 0L
    private var highestActiveCId = 0L


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentAccountsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_accounts, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Set the highestId's (used in navigateToNewExpense() to check that ANY accounts/categories exist)
        viewModel.highestIdActiveAccount.observe(viewLifecycleOwner, { highestActiveAId = it?: 0L })
        viewModel.highestIdActiveCategory.observe(viewLifecycleOwner, { highestActiveCId = it?: 0L })

        // RecyclerView stuff
        val adapter = AccountAdapter({ aId, aAutoclear ->  // incomeClickListener
            IncomeDialog.newInstance(aId, aAutoclear).show(childFragmentManager, "IncomeDialogTag")
        }, { aId, aAutoclear ->  // transferClickListener
            TransferDialog.newInstance(aId, aAutoclear).show(childFragmentManager, "TransferDialogTag")
        })
        binding.faRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            binding.faHint.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.faHintArrow.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            adapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
            binding.faRecycler.visibility = View.VISIBLE  // Draw the recycler only after it has loaded its list so that the layoutAnimation runs reliably
        } })

        // Toolbar and fab
        setHasOptionsMenu(true)  // Essentially causes a call to onCreateOptionsMenu, which creates the options menu in the toolbar
        viewModel.navigateToAddExpense.observe(viewLifecycleOwner, { if(it) {
            navigateToNewExpense()
            viewModel.onNavigatedToNewt()  // Reset the observed navigateToAddExpense LiveData<Boolean>
        } })
        binding.faRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {  // Hide the FAB when scrolling down, show it when scrolling up
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.faFab.visibility == View.VISIBLE) {
                    binding.faFab.hide()
                } else if (dy < 0 && binding.faFab.visibility != View.VISIBLE) {
                    binding.faFab.show()
                }
            }
        })

        // Total balance stuff
        viewModel.allAccounts.observe(viewLifecycleOwner, {
            var totalBalance = 0.0
            viewModel.allAccounts.value?.let {
                for (account in viewModel.allAccounts.value!!) {  // Sum over all the accounts to find the total balance
                    totalBalance += (account.aCleared + account.aPending)
                }
            }
            if (-0.005 <= totalBalance && totalBalance < 0.005) {  // Not testing for == 0.0 because of Double imprecision
                binding.faTotalBg.visibility = View.GONE
                binding.faTotalLabel.visibility = View.GONE
                binding.faTotalValue.visibility = View.GONE
            } else {
                binding.faTotalBg.visibility = View.VISIBLE
                binding.faTotalLabel.visibility = View.VISIBLE
                binding.faTotalValue.visibility = View.VISIBLE
                binding.faTotalValue.text = currencyFormat(totalBalance, requireContext())
            }
        })

        return binding.root
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
        navController.navigate(R.id.action_accountsFragment_to_expenseFragment)
    }

    /** Inflate the (toolbar) menu */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_accounts, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /** Handle menu item selection */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_accounts_edit -> {
                findNavController().navigate(R.id.action_accountsFragment_to_accountsEditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}