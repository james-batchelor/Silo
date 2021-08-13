package com.silofinance.silo.budget

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.CategoryAdapter
import com.silofinance.silo.currencyFormat
import com.silofinance.silo.getDouble
import com.silofinance.silo.databinding.FragmentBudgetBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize


@Parcelize  // So that we can pass this fragment ("this" in the MoveMoneyDialog.newInstance functions below) into the move money dialogs, so that they can call setUpTbb()
class BudgetFragment : Fragment(), Parcelable {

    @IgnoredOnParcel
    private lateinit var binding: FragmentBudgetBinding
    @IgnoredOnParcel
    private var highestActiveAId = 0L
    @IgnoredOnParcel
    private var highestActiveCId = 0L


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_budget, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = BudgetViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BudgetViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Set the highestId's (used in navigateToNewExpense() to check that ANY accounts/categories exist)
        viewModel.highestIdActiveAccount.observe(viewLifecycleOwner, { highestActiveAId = it?: 0L })
        viewModel.highestIdActiveCategory.observe(viewLifecycleOwner, { highestActiveCId = it?: 0L })

        // RecyclerView stuff
        val adapter = CategoryAdapter { cId ->  // categoryClickListener
            val moveMoneyDialog = MoveMoneyDialog.newInstance(cId, this)
            moveMoneyDialog.show(childFragmentManager, "MoveMoneyDialogTag")
        }
        binding.fbRecycler.adapter = adapter  // The point of the adapter is to convert the app data into objects/things(/ViewHolders) the RecyclerView can work with
        viewModel.activeCategories.observe(viewLifecycleOwner, { it?.let {
            binding.fbHint.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.fbHintArrow.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            adapter.submitList(it)  // When submitList is called, the ListAdapter diffs the new list against the old one and detects items that were added, removed, moved, or changed. Then the ListAdapter updates the items shown by RecyclerView.
            binding.fbRecycler.visibility = View.VISIBLE  // Draw the recycler only after it has loaded its list so that the layoutAnimation runs reliably
        } })

        setUpTbb()

        // Open the move money dialog when the tbb header is tapped, moving from tbb
        viewModel.moveMoneyFromTbbDialog.observe(viewLifecycleOwner, { if(it) {
            val moveMoneyDialog = MoveMoneyDialog.newInstance(-4, this)  // cId = -4 for the tbb "category"
            moveMoneyDialog.show(childFragmentManager, "MoveMoneyDialogTag")

            viewModel.onOpenedTbbToCategory()  // Reset the observed moveMoneyFromTbbDialog LiveData<Boolean>
        } })

        // Toolbar and fab
        setHasOptionsMenu(true)  // Essentially causes a call to onCreateOptionsMenu, which creates the options menu in the toolbar
        viewModel.navigateToAddExpense.observe(viewLifecycleOwner, { if(it) {
            navigateToNewExpense()
            viewModel.onNavigatedToNewt()  // Reset the observed navigateToAddExpense LiveData<Boolean>
        } })
        binding.fbRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {  // Hide the FAB when scrolling down, show it when scrolling up
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fbFab.visibility == View.VISIBLE) {
                    binding.fbFab.hide()
                } else if (dy < 0 && binding.fbFab.visibility != View.VISIBLE) {
                    binding.fbFab.show()
                }
            }
        })

        return binding.root
    }

    fun setUpTbb() {
        // Hide it all by default
        binding.fbTbbBg.visibility = View.GONE
        binding.fbTbbClick.visibility = View.GONE
        binding.fbTbbValue.visibility = View.GONE
        binding.fbTbbLabel.visibility = View.GONE

        // Then, if tbb is < or > than 0 (with tolerance for loss of precision), make it visible with the right message
        val tbbSharedPref = requireActivity().getSharedPreferences(getString(R.string.tbb_pref_file_key), Context.MODE_PRIVATE)
        val tbb = tbbSharedPref.getDouble("tbb", 0.0)
        if (tbb < -0.005 || tbb >= 0.005) {  // As I'm using doubles, when I expect a value of exactly 0 then it probably won't be
            binding.fbTbbBg.visibility = View.VISIBLE
            binding.fbTbbClick.visibility = View.VISIBLE
            binding.fbTbbValue.visibility = View.VISIBLE
            binding.fbTbbLabel.visibility = View.VISIBLE
            when (tbb > 0) {
                true -> { binding.fbTbbValue.text = currencyFormat(tbb, requireContext())
                    binding.fbTbbLabel.text = "TO BE BUDGETED" }  //todo extract
                false -> {
                    binding.fbTbbValue.text = currencyFormat(-tbb, requireContext())
                    binding.fbTbbLabel.text = "OVER BUDGETED" }  //todo extract
            }
        }
    }

    /** Checks if active accounts and categories exist. If they don't, display a toast. If they do, navigate to new expense */
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
        navController.navigate(R.id.action_budgetFragment_to_expenseFragment)
    }

    /** Inflate the (toolbar) menu */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_budget, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /** Handle menu item selection */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_budget_edit -> {
                findNavController().navigate(R.id.action_budgetFragment_to_budgetEditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}