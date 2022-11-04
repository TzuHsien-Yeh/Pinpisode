package com.tzuhsien.immediat.notelist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.allViews
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.immediat.databinding.ItemTagBinding

class TagAdapter(
//    private val uiState: NoteListUiState
private val onClickListener: OnTagClickListener
): ListAdapter<String, TagAdapter.TagViewHolder>(DiffCallback) {
    class TagViewHolder(private val binding: ItemTagBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(tag: String){
            binding.textTag.text = tag
//            binding.cardTag.visibility = View.VISIBLE
//            binding.cardTag.setOnClickListener {
//                binding.cardTag.visibility = View.GONE
//                uiState.onTagClick(tag)
//            }

//            binding.chipTag.text = tag
//            binding.chipTag.setOnCheckedChangeListener { compoundButton, b ->
//                if (b){
//                    binding.chipGroupTag.view(itemView)
//                    uiState.onTagClick(tag)
//                } else {
//                    binding.chipGroupTag.allViews.
//                }
//            }
////            binding.chipGroupTag.setOnCheckedStateChangeListener { group, checkedIds ->
////                if (checkedIds.isNotEmpty()){
////                     group.removeView(itemView)
////                } else {
////                    binding.chipTag.visibility = View.VISIBLE
////                }
////            }
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