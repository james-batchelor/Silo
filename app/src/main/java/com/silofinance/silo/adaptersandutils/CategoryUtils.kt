package com.silofinance.silo.adaptersandutils

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.silofinance.silo.R
import com.silofinance.silo.currencyFormat
import com.silofinance.silo.db.Category


/** Binding utils:
 * So here we have all the different functions which the RecyclerView uses to put data into each ViewHolder. In CategoryAdapter, when bind() is called, the
 * binding category (declared in the list_item_category layout) takes an category object. Then, the different views that the ViewHolder has (eg the
 * TextView which holds the name) are able to run their functions given as app:xyz:"@{category}".
 *
 * The category argument gets passed through as an argument for the corresponding function, which is found here in this file. The xyz is found as an argument
 * to a @BindingAdapter tag. The function name (eg setCategoryName) isn't so relevant, but is used automatically in the generated DataBinding files.
 */

@BindingAdapter("categoryEmoji")
fun TextView.setCategoryEmoji(item: Category?) {
    item?.let {
        text = item.cEmoji
    }
}

@BindingAdapter("categoryName")
fun TextView.setCategoryName(item: Category?) {
    item?.let {
        text = item.cName
    }
}

@BindingAdapter("categoryAvailable")
fun TextView.setCategoryAvailable(item: Category?) {
    item?.let {
        text = currencyFormat(item.cAvailable, context)
        when {
            item.cAvailable >= 0.005 -> setTextColor(ContextCompat.getColor(context, R.color.green))
            item.cAvailable < -0.005 -> setTextColor(ContextCompat.getColor(context, R.color.red))
            else -> setTextColor(Color.BLACK)  // As I'm using doubles, when I expect a value of exactly 0 then it probably won't be
        }
    }
}

@BindingAdapter("categoryAllocated")
fun TextView.setCategoryAllocated(item: Category?) {
    item?.let {
        text = currencyFormat(item.cAllocated, context)
        when {
            item.cAllocated >= 0.005 -> setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            item.cAllocated < -0.005 -> setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            else -> setTextColor(Color.BLACK)  // As I'm using doubles, when I expect a value of exactly 0 then it probably won't be
        }
    }
}

@BindingAdapter("categorySelected")
fun View.setCategorySelected(item: Category?) {
    item?.let {
        visibility = when (item.cSelected) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }
}