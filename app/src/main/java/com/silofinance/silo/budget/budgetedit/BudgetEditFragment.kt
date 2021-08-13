package com.silofinance.silo.budget.budgetedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.SectionsPagerAdapter
import com.silofinance.silo.databinding.FragmentBudgetEditBinding


/** This fragment hosts the tab view and the viewpager which in turn hosts the 'active' and 'hidden' fragments */
class BudgetEditFragment : Fragment() {

    private lateinit var binding: FragmentBudgetEditBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_budget_edit, container, false)

        return binding.root
    }

    /** This really just sets up the tabs. It needs to run after onCreateView because it needs to refer to an already-created view pager */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val editBudgetTabAdapter = SectionsPagerAdapter(childFragmentManager)
        editBudgetTabAdapter.addFragment(BudgetActiveFragment())
        editBudgetTabAdapter.addFragment(BudgetHiddenFragment())
        binding.fbeViewPager.adapter = editBudgetTabAdapter
        binding.fbeTabLayout.setupWithViewPager(binding.fbeViewPager)

        binding.fbeTabLayout.getTabAt(0)!!.text = resources.getString(R.string.activeTabTitle)
        binding.fbeTabLayout.getTabAt(1)!!.text = resources.getString(R.string.hiddenTabTitle)

        super.onActivityCreated(savedInstanceState)
    }
}