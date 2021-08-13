package com.silofinance.silo.budget.budgetedit

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.CategoryActiveAdapter
import com.silofinance.silo.budget.BudgetViewModel
import com.silofinance.silo.budget.BudgetViewModelFactory
import com.silofinance.silo.databinding.TabBudgetActiveBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class BudgetActiveFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: TabBudgetActiveBinding = DataBindingUtil.inflate(inflater, R.layout.tab_budget_active, container, false)
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
        val adapter = CategoryActiveAdapter({ cId ->  // swapClickListener
            SwapCategoriesDialog.newInstance(cId).show(childFragmentManager, "SwapCategoriesDialogTag")
        }, { category ->  // hideClickListener
            if (-0.005 <= category.cAvailable && category.cAvailable < 0.005) {  // Not testing for == 0.0 because of Double imprecision
                viewModel.categoryFlipActive(category)
                Toast.makeText(context, "Category hidden", Toast.LENGTH_SHORT).show()  // todo extract
            } else {
                Toast.makeText(context, "Can't hide money-holding categories", Toast.LENGTH_SHORT).show()  // todo extract
            }
        }, { cId, cEmoji, cName ->  // textClickListener
            EditEmojiNameCategoryDialog.newInstance(cId, cEmoji, cName).show(childFragmentManager, "EditEmojiNameCategoryDialogTag")
        })
        binding.tbaRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things the RecyclerView can work with
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let {
            binding.tbaHint.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.tbaHintArrow.visibility = when (it.isEmpty()) {
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
        inflater.inflate(R.menu.menu_budget_edit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /** Handle menu item selection */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_budget_edit_add -> {
                val createCategoryDialog = CreateCategoryDialog.newInstance()
                createCategoryDialog.show(childFragmentManager, "CreateCategoryDialogTag")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}