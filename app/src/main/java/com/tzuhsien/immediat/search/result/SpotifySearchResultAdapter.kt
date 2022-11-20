package com.tzuhsien.immediat.search.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.SpotifyItem
import com.tzuhsien.immediat.databinding.ItemSpotifySearchResultBinding

class SpotifySearchResultAdapter(
    private val uiState: SearchResultUiState
): ListAdapter<SpotifyItem, SpotifySearchResultAdapter.SpResultViewHolder>(DiffCallback) {
    class SpResultViewHolder(private val binding: ItemSpotifySearchResultBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(resultItem: SpotifyItem){

            if (resultItem.images.isNotEmpty()) {
                Glide.with(binding.imgThumbnail)
                    .load(resultItem.images[0].url)
                    .apply(
                        RequestOptions
                            .placeholderOf(R.drawable.app_icon)
                            .error(R.drawable.app_icon)
                    )
                    .into(binding.imgThumbnail)
            } else {
                binding.imgThumbnail.setImageResource(R.drawable.app_icon)
            }

            binding.textTitle.text = resultItem.name
            binding.textShowName.text = if (null != resultItem.show) {
                resultItem.show.name
            } else if (null != resultItem.album) {
                val artistList = mutableListOf<String>()
                for (artist in resultItem.album.artists) {
                    artistList.add(artist.name)
                }

                artistList.toString().trimStart().trimEnd()

            } else { "" }

            binding.textType.text = when (resultItem.type) {
                "episode" -> "Podcast"
                "track" -> "Song"
                else -> ""
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotifySearchResultAdapter.SpResultViewHolder {
        return SpotifySearchResultAdapter.SpResultViewHolder(ItemSpotifySearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SpotifySearchResultAdapter.SpResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onSpotifyItemClick(item)
        }
        holder.bind(item)
    }
}