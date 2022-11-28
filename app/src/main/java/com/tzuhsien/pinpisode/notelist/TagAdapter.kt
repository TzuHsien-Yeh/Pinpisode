package com.tzuhsien.pinpisode.notelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.databinding.ItemTagBinding

class TagAdapter(
private val onClickListener: OnTagClickListener
): ListAdapter<String, TagAdapter.TagViewHolder>(DiffCallback) {
    class TagViewHolder(private val binding: ItemTagBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(tag: String){
            binding.textTag.text = tag
            if (adapterPosition == 0) {
                val param = binding.cardTag.layoutParams as ViewGroup.MarginLayoutParams
                param.marginStart = 18
                binding.cardTag.layoutParams = param
            } else {
                val param = binding.cardTag.layoutParams as ViewGroup.MarginLayoutParams
                param.marginStart = 5
                binding.cardTag.layoutParams = param
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder(ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tagItem = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(tagItem)
        }
        holder.bind(tagItem)
    }

    class OnTagClickListener(val clickListener: (String) -> Unit) {
        fun onClick(tag: String) {
            clickListener(tag)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

    }

}