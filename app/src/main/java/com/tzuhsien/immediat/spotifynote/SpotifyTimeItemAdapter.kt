package com.tzuhsien.immediat.spotifynote

import android.graphics.Typeface
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

            var deleteViewState = 0

            fun resetDeleteBtnStatus() {
                deleteViewState = 0
                binding.deleteTimeItem.text = "Delete"
                binding.deleteTimeItem.typeface = Typeface.DEFAULT
            }

            fun confirmDeleteStatus() {
                deleteViewState = 1
                binding.deleteTimeItem.text = "Confirm on delete"
                binding.deleteTimeItem.typeface = Typeface.DEFAULT_BOLD
            }

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
                } else {
                    resetDeleteBtnStatus()
                }
            }
            contentView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    timeItem.text = contentView.text.toString()
                    uiState.onItemContentChanged(timeItem)
                } else {
                    resetDeleteBtnStatus()
                }
            }

            binding.deleteTimeItem.typeface = when (deleteViewState) {
                0 -> Typeface.DEFAULT
                1 -> Typeface.DEFAULT_BOLD
                else -> Typeface.DEFAULT
            }

            binding.deleteTimeItem.setOnClickListener {
                when (deleteViewState) {
                    0 -> {
                        confirmDeleteStatus()
                    }
                    1 -> {
                        uiState.onItemToDelete(timeItem)
                        resetDeleteBtnStatus()
                    }
                }
            }

            // Play the timeTime when onClicked
            binding.textTimeStart.setOnClickListener {
                uiState.onTimeClick(timeItem)

                resetDeleteBtnStatus()
            }

            binding.textTimeEnd.setOnClickListener {
                uiState.onTimeClick(timeItem)

                resetDeleteBtnStatus()
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
            binding.deleteTimeItem.visibility = if (uiState.canEdit) View.VISIBLE else View.GONE

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