package com.silofinance.silo.budget.budgetedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.CategoryHiddenAdapter
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.TabBudgetHiddenBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class BudgetHiddenFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: TabBudgetHiddenBinding = DataBindingUtil.inflate(inflater, R.layout.tab_budget_hidden, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // RecyclerView stuff
        val adapter = CategoryHiddenAdapter({ cId ->  // mergeClickListener
            MergeCategoryDialog.newInstance(cId).show(childFragmentManager, "MergeCategoryDialogTag")
        }, { category ->  // activateClickListener
            viewModel.categoryFlipActive(category)
        }, { cId ->  // deleteClickListener
            DeleteCategoryDialog.newInstance(cId).show(childFragmentManager, "DeleteCategoryDialogTag")
        }, { cId, cEmoji, cName ->  // textClickListener
            EditEmojiNameCategoryDialog.newInstance(cId, cEmoji, cName).show(childFragmentManager, "EditEmojiNameCategoryDialogTag")
        })
        binding.tbhRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things the RecyclerView can work with
        viewModel.hiddenCategories.observe(viewLifecycleOwner, { it?.let {
            adapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
        } })

        return binding.root
    }
}