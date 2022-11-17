package com.tzuhsien.immediat.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.data.model.ItemX
import com.tzuhsien.immediat.databinding.ItemYtSearchResultBinding
import com.tzuhsien.immediat.ext.utcToLocalTime

class SearchResultAdapter(
    private val uiState: SearchUiState
): ListAdapter<ItemX, SearchResultAdapter.YtResultViewHolder>(DiffCallback) {
    class YtResultViewHolder(private val binding: ItemYtSearchResultBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(resultItem: ItemX, uiState: SearchUiState){

            Glide.with(binding.imgThumbnail)
                .load(resultItem.snippet.thumbnails.high.url)
                .into(binding.imgThumbnail)

            binding.textTitle.text = resultItem.snippet.title
            binding.textChannelName.text = resultItem.snippet.channelTitle
            binding.textPublishedTime.text = resultItem.snippet.publishedAt.utcToLocalTime()
        }
    }


    companion object DiffCallback : DiffUtil.ItemCallback<ItemX>() {
        override fun areItemsTheSame(oldItem: ItemX, newItem: ItemX): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: ItemX, newItem: ItemX): Boolean {
            return oldItem.snippet == newItem.snippet &&
                    oldItem.id == newItem.id
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YtResultViewHolder {
        return YtResultViewHolder(ItemYtSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: YtResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onItemClick(item)
        }
        holder.bind(item, uiState)
    }
}