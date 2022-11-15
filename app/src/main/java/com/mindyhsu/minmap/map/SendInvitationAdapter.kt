package com.mindyhsu.minmap.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.bindImage
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.databinding.ItemSendInvitationBinding

class SendInvitationAdapter :
    ListAdapter<User, SendInvitationAdapter.SendInvitationViewHolder>(SendInvitationDiffCallback()) {

    class SendInvitationViewHolder(private var binding: ItemSendInvitationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: User) {
            binding.sendInvitationFriendNameText.text = item.name
            bindImage(binding.sendInvitationFriendImage, item.image)
        }
    }

    class SendInvitationDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SendInvitationViewHolder {
        return SendInvitationViewHolder(
            ItemSendInvitationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SendInvitationViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend)
    }
}