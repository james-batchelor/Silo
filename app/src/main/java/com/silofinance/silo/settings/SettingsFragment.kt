package com.silofinance.silo.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.silofinance.silo.R
import com.silofinance.silo.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentSettingsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        childFragmentManager.beginTransaction().replace(R.id.settings_container, SettingsPreferenceFragment()).commit()

        return binding.root
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}

// Currency symbol, currency placement (before/after), PIN
// choose what day of the month the last budget reset happens on (1 to 28, then maybe last/2nd last/...) or even choose if the budget resets every week or month