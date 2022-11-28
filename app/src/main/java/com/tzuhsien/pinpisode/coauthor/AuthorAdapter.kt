package com.tzuhsien.pinpisode.coauthor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tzuhsien.pinpisode.data.model.UserInfo
import com.tzuhsien.pinpisode.databinding.ItemAuthorBinding

class AuthorAdapter: ListAdapter<UserInfo, AuthorAdapter.AuthorViewHolder>(DiffCallback) {

    class AuthorViewHolder(private val binding: ItemAuthorBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(user: UserInfo){
            Glide.with(binding.imgAuthorPic)
                .load(user.pic)
                .into(binding.imgAuthorPic)
        }
    }
    companion object DiffCallback : DiffUtil.ItemCallback<UserInfo>() {
        override fun areItemsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.name == newItem.name &&
                    oldItem.pic == newItem.pic
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorViewHolder {
        return AuthorViewHolder(ItemAuthorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: AuthorViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

}