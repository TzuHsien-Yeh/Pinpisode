package com.tzuhsien.immediat.notelist

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.databinding.ItemNoteBinding

class NoteAdapter (
    private val onClickListener: OnNoteClickListener,
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback) {

    class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(note: Note) {
            Glide.with(binding.imgThumbnail)
                .load(note.thumbnails)
                .into(binding.imgThumbnail)
            binding.textSourceTitle.text = note.title
            binding.textDigest.text = note.digest
            val timeAgo = DateUtils.getRelativeTimeSpanString(note.lastEditTime)
            binding.textLastEditTime.text = timeAgo
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
            onClickListener.onClick(note)
        }
        holder.bind(note)
    }

    class OnNoteClickListener(val clickListener: (note: Note) -> Unit) {
        fun onClick(note: Note) {
            clickListener(note)
        }
    }
}