package com.silofinance.silo.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.silofinance.silo.R
import com.silofinance.silo.databinding.DialogOkButtonInfoBinding


class OkButtonInfoDialog : DialogFragment() {

    companion object{
        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance() : OkButtonInfoDialog {
            return OkButtonInfoDialog()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogOkButtonInfoBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_ok_button_info, container, false)

        // Closes the dialog
        binding.dobiOk.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}