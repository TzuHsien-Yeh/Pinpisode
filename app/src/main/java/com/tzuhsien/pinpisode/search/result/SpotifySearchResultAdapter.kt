package com.tzuhsien.pinpisode.search.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.SpotifyItem
import com.tzuhsien.pinpisode.databinding.ItemOpenSpotifyBinding
import com.tzuhsien.pinpisode.databinding.ItemSpotifyLogoBinding
import com.tzuhsien.pinpisode.databinding.ItemSpotifySearchResultBinding
import com.tzuhsien.pinpisode.ext.glide

private const val TYPE_TOP : Int = 0x00
private const val TYPE_END : Int = 0x01
private const val TYPE_LIST : Int = 0x02


class SpotifySearchResultAdapter(
    private val uiState: SearchResultUiState
): ListAdapter<SpotifyItem, RecyclerView.ViewHolder>(DiffCallback) {

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

    class SpotifyLogoViewHolder(private val binding: ItemSpotifyLogoBinding): RecyclerView.ViewHolder(binding.root)
    class SpotifyLinkViewHolder(private val binding: ItemOpenSpotifyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind() {}
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            (itemCount - 1) -> TYPE_END
            0 -> TYPE_TOP
            else -> TYPE_LIST
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            TYPE_TOP -> SpotifyLogoViewHolder(ItemSpotifyLogoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ))
            TYPE_LIST -> SpResultViewHolder(ItemSpotifySearchResultBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))

            TYPE_END -> SpotifyLinkViewHolder(ItemOpenSpotifyBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
            else -> SpResultViewHolder(ItemSpotifySearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return when (holder) {
            is SpResultViewHolder -> {
                val item = getItem(position)
                holder.itemView.setOnClickListener {
                    uiState.onSpotifyItemClick(item)
                }
                holder.bind(item)
            }
            is SpotifyLinkViewHolder -> {
                holder.itemView.setOnClickListener {
                    uiState.openSpotify()
                }
            }
            else -> {}
        }
    }
}