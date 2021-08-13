package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.silofinance.silo.R
import com.silofinance.silo.adaptersandutils.SectionsPagerAdapter
import com.silofinance.silo.databinding.FragmentAccountsEditBinding


/** This fragment hosts the tab view and the viewpager which in turn hosts the 'active' and 'hidden' fragments */
class AccountsEditFragment : Fragment() {

    private lateinit var binding: FragmentAccountsEditBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_accounts_edit, container, false)

        return binding.root
    }

    /** This really just sets up the tabs. It needs to run after onCreateView because it needs to refer to an already-created view pager */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val editAccountsTabAdapter = SectionsPagerAdapter(childFragmentManager)
        editAccountsTabAdapter.addFragment(AccountsActiveFragment())
        editAccountsTabAdapter.addFragment(AccountsHiddenFragment())
        binding.faeViewPager.adapter = editAccountsTabAdapter
        binding.faeTabLayout.setupWithViewPager(binding.faeViewPager)

        binding.faeTabLayout.getTabAt(0)!!.text = resources.getString(R.string.activeTabTitle)
        binding.faeTabLayout.getTabAt(1)!!.text = resources.getString(R.string.hiddenTabTitle)

        super.onActivityCreated(savedInstanceState)
    }
}