package com.tzuhsien.immediat.youtubenote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.databinding.ItemTimeCardBinding
import com.tzuhsien.immediat.ext.convertDurationToDisplay
import com.tzuhsien.immediat.ext.customNoteEditView

class TimeItemAdapter(
    private val onClickListener: OnClickListener,
//    private val uiState: YouTubeNoteUiState
    ) :
    ListAdapter<TimeItem, TimeItemAdapter.TimeItemViewHolder>(DiffCallback) {

    class OnClickListener(val clickListener: (timeItem: TimeItem) -> Unit) {
        fun onClick(timeItem: TimeItem) = clickListener(timeItem)
    }

    class TimeItemViewHolder(private var binding: ItemTimeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context

        fun bind(timeItem: TimeItem) {
            binding.textTimeStart.text = timeItem.startAt.convertDurationToDisplay()
            if (null != timeItem.endAt) {
                binding.textTimeEnd.visibility = View.VISIBLE
                binding.textTimeEnd.text = context.getString(R.string.end_time_format,
                    timeItem.endAt.convertDurationToDisplay())
            } else {
                binding.textTimeEnd.visibility = View.GONE
            }
            customNoteEditView(binding.editTextInputText, binding.textContent, timeItem.text)
            customNoteEditView(binding.editTextItemTitle, binding.textTimeItemTitle, timeItem.title)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TimeItem>() {
        override fun areItemsTheSame(oldItem: TimeItem, newItem: TimeItem): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: TimeItem, newItem: TimeItem): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.startAt == newItem.startAt &&
                    oldItem.endAt == newItem.endAt
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TimeItemAdapter.TimeItemViewHolder {
        return TimeItemAdapter.TimeItemViewHolder(ItemTimeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TimeItemViewHolder, position: Int) {
        val item = getItem(position) as TimeItem
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        return holder.bind(item)
    }
}