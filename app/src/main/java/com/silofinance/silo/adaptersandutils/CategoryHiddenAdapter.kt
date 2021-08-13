package com.silofinance.silo.adaptersandutils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.silofinance.silo.databinding.ListItemCategoryHiddenBinding
import com.silofinance.silo.db.Category


/** So this ListAdapter extender takes a Category as an argument, and uses a generic ViewHolder defined in this same file at the end. ListAdapter is similar to
 * RecyclerView.Adapter, but implements getItemCount()/getItem()/etc on its own (and submitList() over in the fragment) */
class CategoryHiddenAdapter(
    private val mergeClickListener: (cId: Long) -> Unit,
    private val activateClickListener: (category: Category) -> Unit,
    private val deleteClickListener: (cId: Long) -> Unit,
    private val textClickListener: (cId: Long, cEmoji: String, cName: String) -> Unit
) : ListAdapter<Category, CategoryHiddenAdapter.ViewHolder>(CategoryHiddenDiffCallback()) {

    /** RecyclerView calls this to create a ViewHolder to represent an item. viewType is used to distinguish different views (like TextView vs ImageView). */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)  // So get a ViewHolder from the ViewHolder class in this file
    }

    /** RecyclerView calls this to bind/display data to/in the relevant ViewHolder */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, mergeClickListener, activateClickListener, deleteClickListener, textClickListener)  // Defined below
    }


    /** A ViewHolder contains views and data, but also metadata used by a RecyclerView for correct positioning, animation, etc */
    class ViewHolder private constructor(val binding: ListItemCategoryHiddenBinding) : RecyclerView.ViewHolder(binding.root) {

        /** Bind data to the item (ie from a database). See the block comment in CategoryUtils.kt for more info */
        fun bind(
            item: Category,
            mergeClickListener: (cId: Long) -> Unit,
            activateClickListener: (category: Category) -> Unit,
            deleteClickListener: (cId: Long) -> Unit,
            textClickListener: (cId: Long, cEmoji: String, cName: String) -> Unit
        ) {
            binding.category = item

            binding.lichEmoji.setOnClickListener { textClickListener(item.cId, item.cEmoji, item.cName) }
            binding.lichName.setOnClickListener { textClickListener(item.cId, item.cEmoji, item.cName) }
            binding.lichMerge.setOnClickListener { mergeClickListener(item.cId) }
            binding.lichActivate.setOnClickListener { activateClickListener(item) }
            binding.lichDelete.setOnClickListener { deleteClickListener(item.cId) }

            binding.executePendingBindings()  // Optimisation thing
        }


        companion object {  // Needs to be in a companion object so it can be called on the ViewHolder class, not called on a ViewHolder instance
            /** Returns a new ViewHolder instance */
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCategoryHiddenBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }


    /** DiffUtil uses these two methods to figure out how the list and items have changed */
    class CategoryHiddenDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.cId == newItem.cId  // If the items have the same Id, they are the same item, so return true
        }
        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem  // Check whether oldItem and newItem contain the same data (for all fields because the 'items' are data classes)
        }
    }
}