package com.tzuhsien.pinpisode.notelist

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.databinding.ItemNoteBinding
import com.tzuhsien.pinpisode.ext.glide

class NoteAdapter (
    private val uiState: NoteListUiState
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback) {

    class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(note: Note) {
            val context = binding.root.context
            binding.imgThumbnail.glide(note.thumbnail)
            @DrawableRes val platformIcon = if (note.source == Source.SPOTIFY.source) R.drawable.ic_spotify else R.drawable.ic_youtube
            binding.icPlatform.setImageResource(platformIcon)
            binding.textSourceTitle.text = note.title
            binding.textDigest.text = note.digest
            val timeAgo = DateUtils.getRelativeTimeSpanString(note.lastEditTime)
            binding.textLastEditTime.text = timeAgo
            binding.textNumberOfCoauthors.visibility = if (note.authors.size == 1) View.GONE else View.VISIBLE
            binding.textNumberOfCoauthors.text = if (note.authors.size > 2) {
                context.getString(
                    R.string.number_of_coauthors, (note.authors.size.minus(1))
                ) + "s"
            } else {
                context.getString(
                    R.string.number_of_coauthors, (note.authors.size.minus(1))
                )
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.digest == newItem.digest &&
                    oldItem.lastEditTime == newItem.lastEditTime
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.itemView.setOnClickListener {
            uiState.onNoteClicked(note)
        }
        holder.bind(note)
    }
}