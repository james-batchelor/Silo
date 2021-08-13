package com.silofinance.silo.adaptersandutils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.silofinance.silo.R
import com.silofinance.silo.db.Account


class AccountSpinnerAdapter(val context: Context, var list: List<Account>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    override fun getItem(position: Int): Any? = list[position]
    override fun getCount(): Int = list.size
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.spinner_item, parent, false)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        viewHolder.spinnerEmoji.text = list[position].aEmoji
        viewHolder.spinnerName.text = list[position].aName

        return view
    }

    private class ViewHolder(view: View?) {
        val spinnerEmoji: TextView = view?.findViewById(R.id.spinner_emoji) as TextView
        val spinnerName: TextView = view?.findViewById(R.id.spinner_name) as TextView
    }
}