package com.tzuhsien.immediat.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.data.model.Item
import com.tzuhsien.immediat.databinding.ItemYtTrendingBinding
import com.tzuhsien.immediat.ext.utcToLocalTime

class YtTrendingAdapter(
    private val uiState: SearchUiState
): ListAdapter<Item, YtTrendingAdapter.YtResultViewHolder>(DiffCallback) {
    class YtResultViewHolder(private val binding: ItemYtTrendingBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(resultItem: Item, uiState: SearchUiState){

            Glide.with(binding.imgThumbnail)
                .load(resultItem.snippet.thumbnails.high.url)
                .into(binding.imgThumbnail)

            binding.textTitle.text = resultItem.snippet.title
            binding.textChannelName.text = resultItem.snippet.channelTitle
            binding.textPublishedTime.text = resultItem.snippet.publishedAt.utcToLocalTime()
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
        holder.bind(item, uiState)
    }
}