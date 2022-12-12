package com.tzuhsien.pinpisode.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.data.model.Item
import com.tzuhsien.pinpisode.databinding.ItemYtTrendingBinding
import com.tzuhsien.pinpisode.ext.glide
import com.tzuhsien.pinpisode.ext.utcToLocalTime

class YtTrendingAdapter(
    private val uiState: SearchUiState
): ListAdapter<Item, YtTrendingAdapter.YtResultViewHolder>(DiffCallback) {
    class YtResultViewHolder(private val binding: ItemYtTrendingBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(resultItem: Item){
            binding.imgThumbnail.glide(resultItem.snippet.thumbnails.high.url)
            binding.textTitle.text = resultItem.snippet.title
            binding.textChannelName.text = resultItem.snippet.channelTitle
            binding.textPublishedTime.text = resultItem.snippet.publishedAt.utcToLocalTime()

            if(adapterPosition == 0) {
                val param = binding.cardYtTrending.layoutParams as ViewGroup.MarginLayoutParams
                param.marginStart = 24
                binding.cardYtTrending.layoutParams = param
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.snippet == newItem.snippet &&
                    oldItem.id == newItem.id
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YtResultViewHolder {
        return YtResultViewHolder(ItemYtTrendingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: YtResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onTrendingVideoClick(item)
        }
        holder.bind(item)
    }
}