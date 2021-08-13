package com.silofinance.silo.accounts.accountsedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.silofinance.silo.R
import com.silofinance.silo.databinding.DialogCreateAccountHelpBinding


class CreateAccountHelpDialog : DialogFragment() {

    companion object{
        private const val ARG_TYPE = "createAccountHelpDialog_type"

        // The dialog is created by calling Dialog.newInstance(). This protects the dialog from reconfiguration (eg rotating the screen) crashes
        fun newInstance(type: String) : CreateAccountHelpDialog {
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            val fragment = CreateAccountHelpDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var type: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            type = requireArguments().getString(ARG_TYPE).toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: DialogCreateAccountHelpBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_create_account_help, container, false)

        binding.dcahTitle.text = when (type) {
            "CREDIT" -> "Credit Cards"
            "OVERDRAWN" -> "Overdrawn Accounts"
            "LOAN" -> "Loans"
            else -> ""
        }

        binding.dcahText.text = when (type) {
            "CREDIT" -> getString(R.string.dcah_credit_text)
            "OVERDRAWN" -> getString(R.string.dcah_overdrawn_text)
            "LOAN" -> getString(R.string.dcah_loan_text)
            else -> ""
        }

        // Closes the dialog
        binding.dcahOk.setOnClickListener{ dismiss() }  // Close the dialog

        return binding.root
    }
}