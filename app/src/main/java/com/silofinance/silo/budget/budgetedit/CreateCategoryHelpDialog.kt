package com.silofinance.silo.budget.budgetedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.silofinance.silo.R
import com.silofinance.silo.databinding.DialogCreateCategoryHelpBinding


class CreateCategoryHelpDialog : DialogFragment() {

    companion object{
        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance() : CreateCategoryHelpDialog {
            return CreateCategoryHelpDialog()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogCreateCategoryHelpBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_create_category_help, container, false)

        // Closes the dialog
        binding.dcchOk.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}