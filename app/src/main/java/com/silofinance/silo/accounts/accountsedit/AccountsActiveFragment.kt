package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.accounts.AccountsViewModel
import com.silofinance.silo.accounts.AccountsViewModelFactory
import com.silofinance.silo.adaptersandutils.AccountActiveAdapter
import com.silofinance.silo.databinding.TabAccountsActiveBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb
import com.silofinance.silo.hideKeyboard


class AccountsActiveFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: TabAccountsActiveBinding = DataBindingUtil.inflate(inflater, R.layout.tab_accounts_active, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AccountsViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AccountsViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // RecyclerView stuff
        val adapter = AccountActiveAdapter ({ aId ->  // swapClickListener
            SwapAccountsDialog.newInstance(aId).show(childFragmentManager, "SwapAccountsDialogTag")
        }, { account ->  // hideClickListener
            val balance = (account.aCleared + account.aPending)
            if (-0.005 <= balance && balance < 0.005) {  // Not testing for == 0.0 because of Double imprecision
                viewModel.accountFlipActive(account)
                Toast.makeText(context, "Account hidden", Toast.LENGTH_SHORT).show()  // todo extract
            } else {
                Toast.makeText(context, "Can't hide nonzero-balance accounts", Toast.LENGTH_SHORT).show()  // todo extract
            }
        }, { aId, aEmoji, aName ->  // textClickListener
            EditEmojiNameAccountDialog.newInstance(aId, aEmoji, aName).show(childFragmentManager, "EditEmojiNameAccountDialogTag")
        }, { aId ->  // flipAutoclearListener
            viewModel.flipAutoclear(aId)
        })
        binding.taaRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.activeAccounts.observe(viewLifecycleOwner, { it?.let {
            binding.taaHint.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.taaHintArrow.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            adapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
        } })

        setHasOptionsMenu(true)  // Essentially causes a call to onCreateOptionsMenu, which creates the options menu in the toolbar

        return binding.root
    }

    /** Inflate the (toolbar) menu */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_accounts_edit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /** Handle menu item selection */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_accounts_edit_add -> {
                hideKeyboard()
                val createAccountDialog = CreateAccountDialog.newInstance()
                createAccountDialog.show(childFragmentManager, "CreateAccountDialogTag")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}