package com.tzuhsien.immediat.youtubenote

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

class TimeItemAdapter(
    private val onClickListener: OnClickListener,
    private val uiState: YouTubeNoteUiState,
) :
    ListAdapter<TimeItem, TimeItemAdapter.TimeItemViewHolder>(DiffCallback) {

    class OnClickListener(val clickListener: (timeItem: TimeItem) -> Unit) {
        fun onClick(timeItem: TimeItem) = clickListener(timeItem)
    }

    class TimeItemViewHolder(private var binding: ItemTimeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context

        fun bind(timeItem: TimeItem, uiState: YouTubeNoteUiState) {
            binding.textTimeStart.text = timeItem.startAt.formatDuration()
            if (null != timeItem.endAt) {
                binding.textTimeEnd.visibility = View.VISIBLE
                binding.textTimeEnd.text =
                    context.getString(R.string.end_time_format, timeItem.endAt.formatDuration())
            } else {
                binding.textTimeEnd.visibility = View.GONE
            }

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

            // TODO:　SET content/title.inputType = TYPE_NULL if (!authorList.contains(UserManager.userId))

//            var editTextState = 0
//            content.setOnClickListener {
//                when (editTextState) {
//                    0 -> {
//                        content.setBackgroundColor(context.getColor(R.color.colorPrimaryVariant))
//                        editTextState = 1
//                    }
//                    1 -> {
//                        binding.editTextInputText.inputType = InputType.TYPE_CLASS_TEXT
//                        editTextState = 1
//                    }
//                    2 -> {
//                        binding.editTextInputText.inputType = TYPE_NULL
//                        editTextState = 0
//                    }
//                }
//            }
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
            holder.itemView.setOnClickListener {
                onClickListener.onClick(item)
            }
            return holder.bind(item, uiState)
        }
    }