package com.silofinance.silo.adaptersandutils

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.silofinance.silo.R
import com.silofinance.silo.currencyFormat
import com.silofinance.silo.db.Account


/** Binding utils:
 * So here we have all the different functions which the RecyclerView uses to put data into each ViewHolder. In AccountAdapter, when bind() is called, the
 * binding account (declared in the list_item_account layout) takes an account object. Then, the different views that the ViewHolder has (eg the
 * TextView which holds the name) are able to run their functions given as app:xyz:"@{account}".
 *
 * The account argument gets passed through as an argument for the corresponding function, which is found here in this file. The xyz is found as an argument
 * to a @BindingAdapter tag. The function name (eg setAccountName) isn't so relevant, but is used automatically in the generated DataBinding files.
 */

@BindingAdapter("accountEmoji")
fun TextView.setAccountEmoji(item: Account?) {
    item?.let {
        text = item.aEmoji
    }
}

@BindingAdapter("accountName")
fun TextView.setAccountName(item: Account?) {
    item?.let {
        text = item.aName
    }
}

@BindingAdapter("accountCleared")
fun TextView.setAccountCleared(item: Account?) {
    item?.let {
        text = currencyFormat(item.aCleared, context)
    }
}

@BindingAdapter("accountPending")
fun TextView.setAccountPending(item: Account?) {
    item?.let {
        text = currencyFormat(item.aPending, context)
    }
}

@BindingAdapter("accountBalance")
fun TextView.setAccountBalance(item: Account?) {
    item?.let {
        val trueBalance: Double = item.aCleared + item.aPending
        text = currencyFormat(trueBalance, context)
        when {
            trueBalance >= 0.005 -> setTextColor(ContextCompat.getColor(context, R.color.green))
            trueBalance < -0.005 -> setTextColor(ContextCompat.getColor(context, R.color.red))
            else -> setTextColor(Color.BLACK)  // As I'm using doubles, when I expect a value of exactly 0 then it probably won't be
        }
    }
}

@BindingAdapter("accountSelected")
fun View.setAccountSelected(item: Account?) {
    item?.let {
        visibility = when (item.aSelected) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }
}