package com.silofinance.silo.adaptersandutils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.silofinance.silo.R
import com.silofinance.silo.currencyFormat
import com.silofinance.silo.db.Transaction
import com.silofinance.silo.epochDayToDateString


/** Binding utils:
 * So here we have all the different functions which the RecyclerView uses to put data into each ViewHolder. In TransactionAdapter, when bind() is called, the
 * binding transaction (declared in the list_item_transaction layout) takes a Transaction object. Then, the different views that the ViewHolder has (eg the
 * TextView which holds the name) are able to run their functions given as app:xyz:"@{transaction}".
 *
 * The transaction argument gets passed through as an argument for the corresponding function, which is found here in this file. The xyz is found as an argument
 * to a @BindingAdapter tag. The function name (eg setTransactionName) isn't so relevant, but is used automatically in the generated DataBinding files.
 */

@BindingAdapter("transactionDate")
fun TextView.setTransactionDate(item: Transaction?) {
    item?.let {
        text = epochDayToDateString(item.tDate, context)
    }
}

@BindingAdapter("transactionAccount")
fun TextView.setTransactionAccount(item: Transaction?) {
    item?.let {
        val aEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
        val emoji = aEmojiSharedPref.getString(item.tAccount.toString(), "") ?: ""
        when (emoji) {
            "" -> {
                text = "😂"  // Placeholder
                visibility = View.INVISIBLE
            }
            else -> {
                text = emoji
                visibility = View.VISIBLE
            }
        }
    }
}

@BindingAdapter("transactionDeletedAccount")
fun ImageView.setTransactionDeletedAccount(item: Transaction?) {
    item?.let {  // 'item' is the bin icon
        visibility = when (item.tAccount) {
            -9L -> View.VISIBLE
            else -> View.GONE
        }
    }
}

@BindingAdapter("transactionTransferArrow")
fun ImageView.setTransactionTransferArrow(item: Transaction?) {
    item?.let {
        visibility = when (item.tType) {
            3 -> View.VISIBLE
            else -> View.GONE
        }
    }
}

@BindingAdapter("transactionCategory")
fun TextView.setTransactionCategory(item: Transaction?) {
    item?.let {
        var emoji = ""
        if (item.tType == 3) {  // So if the transaction is a transfer
            val aEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
            emoji = aEmojiSharedPref.getString(item.tCategory.toString(), "") ?: ""
        } else {
            val cEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)
            emoji = cEmojiSharedPref.getString(item.tCategory.toString(), "") ?: ""
        }
        when (emoji) {
            "" -> {
                text = "😂"  // Placeholder
                visibility = View.INVISIBLE
            }
            else -> {
                text = emoji
                visibility = View.VISIBLE
            }
        }
    }
}

@BindingAdapter("transactionDeletedCategory")
fun ImageView.setTransactionDeletedCategory(item: Transaction?) {
    item?.let {  // item is the bin icon
        visibility = when (item.tCategory) {
            -9L -> View.VISIBLE
            else -> View.GONE
        }
    }
}

@BindingAdapter("transactionNote")
fun TextView.setTransactionNote(item: Transaction?) {
    item?.let {
        text = item.tNote
    }
}

@BindingAdapter("transactionAmount")
fun TextView.setTransactionAmount(item: Transaction?) {
    item?.let {
        text = currencyFormat(item.tAmount, context)
        when {
            item.tAmount >= 0.005 -> setTextColor(ContextCompat.getColor(context, R.color.green))
            item.tAmount < -0.005 -> setTextColor(ContextCompat.getColor(context, R.color.red))
            else -> setTextColor(Color.BLACK)  // As I'm using doubles, when I expect a value of exactly 0 then it probably won't be
        }
        if (item.tType == 3) {
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        }
        if (!item.tCleared) {
            setTextColor(Color.GRAY)
        }
    }
}