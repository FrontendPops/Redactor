package com.tsu.redactorapp.adaptive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.tsu.redactorapp.OnItemClickListener
import com.tsu.redactorapp.R
import com.tsu.redactorapp.models.ImageItem

class ImageAdapter(private val listener: OnItemClickListener) : ListAdapter<ImageItem, ImageAdapter.ViewHolder>(DiffCallback()) {
    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem.resourceId == newItem.resourceId
        }

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)

        fun bindData(item: ImageItem) {
            imageView.setImageResource(item.resourceId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.image_item_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageItem = getItem(position)
        holder.bindData(imageItem)
        holder.itemView.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    fun getItemAtPosition(position: Int): ImageItem {
        return getItem(position)
    }
}

