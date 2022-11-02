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
            binding.textTimeStart.text = timeItem.startAt.formatDuration()
            if (null != timeItem.endAt) {
                binding.textTimeEnd.visibility = View.VISIBLE
                binding.textTimeEnd.text =
                    context.getString(R.string.end_time_format, timeItem.endAt.formatDuration())
            } else {
                binding.textTimeEnd.visibility = View.GONE
            }

            val title = binding.editTextItemTitle
            val content = binding.editTextInputText
            // TODO:　SET content/title.inputType = TYPE_NULL if (!authorList.contains(UserManager.userId))

            content.setText(timeItem.text)
            title.setText(timeItem.text)
            content.setBackgroundColor(context.getColor(R.color.transparent))
            title.setBackgroundColor(context.getColor(R.color.transparent))

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
    ): TimeItemAdapter.TimeItemViewHolder {
        return TimeItemAdapter.TimeItemViewHolder(ItemTimeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TimeItemAdapter.TimeItemViewHolder, position: Int) {
        val item = getItem(position) as TimeItem
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        return holder.bind(item)
    }
}