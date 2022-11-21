package com.tzuhsien.immediat.spotifynote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.databinding.ItemTimeCardBinding
import com.tzuhsien.immediat.ext.formatDuration

class SpotifyTimeItemAdapter(
    private val uiState: SpotifyNoteUiState,
) :
    ListAdapter<TimeItem, SpotifyTimeItemAdapter.TimeItemViewHolder>(DiffCallback) {

    class TimeItemViewHolder(private var binding: ItemTimeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context

        fun bind(timeItem: TimeItem, uiState: SpotifyNoteUiState) {

            binding.textTimeStart.text = timeItem.startAt.formatDuration()
            if (null != timeItem.endAt) {
                binding.textTimeEnd.visibility = View.VISIBLE
                binding.textTimeEnd.text =
                    context.getString(R.string.end_time_format, timeItem.endAt.formatDuration())
            } else {
                binding.textTimeEnd.visibility = View.GONE
            }

            /**
             * EditText instant update
             * */
            val titleView = binding.editTextItemTitle
            val contentView = binding.editTextInputText

            titleView.setBackgroundColor(context.getColor(R.color.transparent))
            contentView.setBackgroundColor(context.getColor(R.color.transparent))

            titleView.setText(timeItem.title)
            contentView.setText(timeItem.text)

            titleView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    timeItem.title = titleView.text.toString()
                    uiState.onItemTitleChanged(timeItem)
                }
            }
            contentView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    timeItem.text = contentView.text.toString()
                    uiState.onItemContentChanged(timeItem)
                }
            }

            // Play the timeTime when onClicked
            binding.textTimeStart.setOnClickListener {
                uiState.onTimeClick(timeItem)
            }

            binding.textTimeEnd.setOnClickListener {
                uiState.onTimeClick(timeItem)
            }

            /** Disable edit functions only if the viewer is one of the authors **/
            titleView.isEnabled = uiState.canEdit
            contentView.isEnabled = uiState.canEdit

            if (uiState.canEdit) {
                contentView.visibility = View.VISIBLE
                contentView.hint = context.getString(R.string.add_some_notes)
            } else {
                if (timeItem.text.isEmpty()) {
                    contentView.visibility = View.GONE
                } else {
                    contentView.visibility = View.VISIBLE
                }
            }
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
    ): TimeItemViewHolder {
        return TimeItemViewHolder(ItemTimeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TimeItemViewHolder, position: Int) {
        val item = getItem(position) as TimeItem

        return holder.bind(item, uiState)
    }
}