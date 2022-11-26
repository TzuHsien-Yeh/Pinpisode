package com.tzuhsien.immediat.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.SpotifyItem
import com.tzuhsien.immediat.databinding.ItemSpotifyLatestContentBinding

class SpContentAdapter(
    private val uiState: SearchUiState
): ListAdapter<SpotifyItem, SpContentAdapter.SpotifyItemViewHolder>(DiffCallback) {
    class SpotifyItemViewHolder(private val binding: ItemSpotifyLatestContentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SpotifyItem, uiState: SearchUiState){

            if (item.images.isNotEmpty()) {
                Glide.with(binding.imgThumbnail)
                    .load(item.images[0].url)
                    .apply(
                        RequestOptions
                            .placeholderOf(R.drawable.app_icon)
                            .error(R.drawable.app_icon)
                    )
                    .into(binding.imgThumbnail)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpContentAdapter.SpotifyItemViewHolder {
        return SpContentAdapter.SpotifyItemViewHolder(ItemSpotifyLatestContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SpotifyItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onSpotifyLatestContentClick(item)
        }
        holder.bind(item, uiState)
    }
}