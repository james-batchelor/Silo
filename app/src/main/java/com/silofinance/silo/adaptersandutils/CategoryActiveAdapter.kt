package com.silofinance.silo.adaptersandutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.databinding.ListItemCategoryActiveBinding
import com.silofinance.silo.db.Category


/** So this ListAdapter extender takes a Category as an argument, and uses a generic ViewHolder defined in this same file at the end. ListAdapter is similar to
 * RecyclerView.Adapter, but implements getItemCount()/getItem()/etc on its own (and submitList() over in the fragment) */
class CategoryActiveAdapter(
    private val swapClickListener: (cId: Long) -> Unit,
    private val hideClickListener: (category: Category) -> Unit,
    private val textClickListener: (cId: Long, cEmoji: String, cName: String) -> Unit
) : ListAdapter<Category, CategoryActiveAdapter.ViewHolder>(CategoryActiveDiffCallback()) {

    /** RecyclerView calls this to create a ViewHolder to represent an item. viewType is used to distinguish different views (like TextView vs ImageView). */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)  // So get a ViewHolder from the ViewHolder class in this file
    }

    /** RecyclerView calls this to bind/display data to/in the relevant ViewHolder */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, swapClickListener, hideClickListener, textClickListener)  // Defined below
    }


    /** A ViewHolder contains views and data, but also metadata used by a RecyclerView for correct positioning, animation, etc */
    class ViewHolder private constructor(val binding: ListItemCategoryActiveBinding) : RecyclerView.ViewHolder(binding.root) {

        /** Bind data to the item (ie from a database). See the block comment in CategoryUtils.kt for more info */
        fun bind(
            item: Category,
            swapClickListener: (cId: Long) -> Unit,
            hideClickListener: (category: Category) -> Unit,
            textClickListener: (cId: Long, cEmoji: String, cName: String) -> Unit
        ) {
            binding.category = item

            binding.licaEmoji.setOnClickListener { textClickListener(item.cId, item.cEmoji, item.cName) }
            binding.licaName.setOnClickListener { textClickListener(item.cId, item.cEmoji, item.cName) }
            binding.licaSwap.setOnClickListener { swapClickListener(item.cId) }
            binding.licaHide.setOnClickListener { hideClickListener(item) }

            binding.executePendingBindings()  // Optimisation thing
        }


        companion object {  // Needs to be in a companion object so it can be called on the ViewHolder class, not called on a ViewHolder instance
            /** Returns a new ViewHolder instance */
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCategoryActiveBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }


    /** DiffUtil uses these two methods to figure out how the list and items have changed */
    class CategoryActiveDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.cId == newItem.cId  // If the items have the same Id, they are the same item, so return true
        }
        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem  // Check whether oldItem and newItem contain the same data (for all fields because the 'items' are data classes)
        }
    }
}