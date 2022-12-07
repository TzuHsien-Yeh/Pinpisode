package com.tzuhsien.pinpisode.search.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.SpotifyItem
import com.tzuhsien.pinpisode.databinding.ItemSpotifySearchResultBinding
import com.tzuhsien.pinpisode.ext.glide

class SpotifySearchResultAdapter(
    private val uiState: SearchResultUiState
): ListAdapter<SpotifyItem, SpotifySearchResultAdapter.SpResultViewHolder>(DiffCallback) {
    class SpResultViewHolder(private val binding: ItemSpotifySearchResultBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(resultItem: SpotifyItem){

            if (resultItem.images.isNotEmpty()) {
                binding.imgThumbnail.glide(resultItem.images[0].url)
            } else if (!resultItem.album?.images.isNullOrEmpty()) {
                binding.imgThumbnail.glide(resultItem.album!!.images[0].url)
            } else {
                binding.imgThumbnail.setImageResource(R.drawable.app_icon)
            }

            binding.textTitle.text = resultItem.name
            binding.textShowName.text = if (null != resultItem.album) {
                val artistList = mutableListOf<String>()
                for (artist in resultItem.album.artists) {
                    artistList.add(artist.name)
                }
                artistList.toString()
            } else { "" }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpResultViewHolder {
        return SpResultViewHolder(ItemSpotifySearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SpResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onSpotifyItemClick(item)
        }
        holder.bind(item)
    }
}