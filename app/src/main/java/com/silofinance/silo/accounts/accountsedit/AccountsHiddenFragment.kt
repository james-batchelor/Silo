package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.accounts.AccountsViewModel
import com.silofinance.silo.accounts.AccountsViewModelFactory
import com.silofinance.silo.adaptersandutils.AccountHiddenAdapter
import com.silofinance.silo.databinding.TabAccountsHiddenBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class AccountsHiddenFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: TabAccountsHiddenBinding = DataBindingUtil.inflate(inflater, R.layout.tab_accounts_hidden, container, false)
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
        val adapter = AccountHiddenAdapter({ aId ->  // mergeClickListener
            MergeAccountDialog.newInstance(aId).show(childFragmentManager, "MergeAccountDialogTag")
        }, { account ->  // activateClickListener
            viewModel.accountFlipActive(account)
            Toast.makeText(context, "Account activated", Toast.LENGTH_SHORT).show()  // todo extract
        }, { aId ->  // deleteClickListener
            DeleteAccountDialog.newInstance(aId).show(childFragmentManager, "DeleteAccountDialogTag")
        }, { aId, aEmoji, aName ->  // textClickListener
            EditEmojiNameAccountDialog.newInstance(aId, aEmoji, aName).show(childFragmentManager, "EditEmojiNameAccountDialogTag")
        })
        binding.tahRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.hiddenAccounts.observe(viewLifecycleOwner, { it?.let {
            adapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
        } })

        return binding.root
    }
}