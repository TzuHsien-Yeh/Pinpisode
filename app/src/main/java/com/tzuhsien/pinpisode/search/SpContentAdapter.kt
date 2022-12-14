package com.tzuhsien.pinpisode.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.SpotifyItem
import com.tzuhsien.pinpisode.databinding.ItemSpotifyLatestContentBinding
import com.tzuhsien.pinpisode.ext.glide

class SpContentAdapter(
    private val uiState: SearchUiState
): ListAdapter<SpotifyItem, SpContentAdapter.SpotifyItemViewHolder>(DiffCallback) {
    class SpotifyItemViewHolder(private val binding: ItemSpotifyLatestContentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SpotifyItem){

            if (item.images.isNotEmpty()) {
                binding.imgThumbnail.glide(item.images[0].url)
                binding.imgThumbnail.alpha = 1F
            } else {
                binding.imgThumbnail.setImageResource(R.drawable.app_icon)
                binding.imgThumbnail.alpha = 0.5F
            }

            binding.textTitle.text = item.name
            binding.textShowName.text = item.show?.name

            if(adapterPosition == 0) {
                val param = binding.cardSpLatestEpisode.layoutParams as ViewGroup.MarginLayoutParams
                param.marginStart = 24
                binding.cardSpLatestEpisode.layoutParams = param
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SpotifyItem>() {
        override fun areItemsTheSame(oldItem: SpotifyItem, newItem: SpotifyItem): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: SpotifyItem, newItem: SpotifyItem): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.id == newItem.id
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotifyItemViewHolder {
        return SpotifyItemViewHolder(ItemSpotifyLatestContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SpotifyItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onSpotifyLatestContentClick(item)
        }
        holder.bind(item)
    }
}