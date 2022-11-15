package com.tzuhsien.immediat.notification

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.Invitation
import com.tzuhsien.immediat.databinding.ItemInvitationBinding

class InvitationAdapter (private val uiState: NotificationUiState) :
    ListAdapter<Invitation, InvitationAdapter.InvitationViewHolder>(DiffCallback) {

    class InvitationViewHolder(private val binding: ItemInvitationBinding):
        RecyclerView.ViewHolder(binding.root) {

            fun bind(invitation: Invitation, uiState: NotificationUiState){
                Glide.with(binding.imgInviterProfile)
                    .load(invitation.inviter!!.pic)
                    .into(binding.imgInviterProfile)

                binding.textInvitationContent.text =
                    binding.textInvitationContent.context.getString(
                        R.string.invitation_content,
                        invitation.inviter!!.name,
                        invitation.note.source
                    )

                binding.textNoteSourceTitle.text = invitation.note.title

                val timeAgo = DateUtils.getRelativeTimeSpanString(invitation.time)
                binding.textNotifReceivedTime.text = timeAgo

                binding.btnAccept.setOnClickListener {
                    uiState.onAcceptClicked(invitation)
                }
                binding.btnDecline.setOnClickListener {
                    uiState.onDeclineClicked(invitation)
                }

            }

    }



    companion object DiffCallback : DiffUtil.ItemCallback<Invitation>() {
        override fun areItemsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.note == newItem.note
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        return InvitationViewHolder(ItemInvitationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        val item = getItem(position)
        return holder.bind(item, uiState)
    }
}