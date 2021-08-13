package com.silofinance.silo.adaptersandutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.databinding.ListItemTransactionBinding
import com.silofinance.silo.db.Transaction


/** So this ListAdapter extender takes a Transaction as an argument, and uses a generic ViewHolder defined in this same file at the end. ListAdapter is similar to
  * RecyclerView.Adapter, but implements getItemCount()/getItem()/etc on its own (and submitList() over in the fragment) */
class TransactionAdapter(private val flipClearedClickListener: (item: Transaction) -> Unit, private val editLongClickListener: (item: Transaction) -> Unit) :
    ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    /** RecyclerView calls this to create a ViewHolder to represent an item. viewType is used to distinguish different views (like TextView vs ImageView). */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)  // So get a ViewHolder from the ViewHolder class in this file
    }

    /** RecyclerView calls this to bind/display data to/in the relevant ViewHolder */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, flipClearedClickListener, editLongClickListener)  // Defined below
    }


    /** A ViewHolder contains views and data, but also metadata used by a RecyclerView for correct positioning, animation, etc */
    class ViewHolder private constructor(val binding: ListItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {

        /** Bind data to the item (ie from a database). See the block comment in TransactionUtils.kt for more info */
        fun bind(item: Transaction,  flipClearedClickListener: (item: Transaction) -> Unit,  editLongClickListener: (item: Transaction) -> Unit
        ) {
            binding.transaction = item
            binding.root.setOnClickListener { flipClearedClickListener(item) }
            binding.root.setOnLongClickListener {
                editLongClickListener(item)
                true}
            binding.executePendingBindings()  // Optimisation thing
        }


        companion object {  // Needs to be in a companion object so it can be called on the ViewHolder class, not called on a ViewHolder instance
            /** Returns a new ViewHolder instance */
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTransactionBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }


    /** DiffUtil uses these two methods to figure out how the list and items have changed */
    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.tId == newItem.tId  // If the items have the same Id, they are the same item, so return true
        }
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem  // Check whether oldItem and newItem contain the same data (for all fields because the 'items' are data classes)
        }
    }
}