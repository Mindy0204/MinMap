package com.mindyhsu.minmap.sendinvitation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.bindImage
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.databinding.ItemSendInvitationBinding

class SendInvitationAdapter(private val uiState: SendInvitationUiState) :
    ListAdapter<User, SendInvitationAdapter.SendInvitationViewHolder>(SendInvitationDiffCallback()) {

    class SendInvitationViewHolder(private var binding: ItemSendInvitationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: User, uiState: SendInvitationUiState) {
            binding.sendInvitationFriendNameText.text = item.name
            bindImage(binding.sendInvitationFriendImage, item.image)

            itemView.setOnClickListener {
                if (binding.sendInvitationChose.visibility == View.GONE) {
                    binding.sendInvitationChose.visibility = View.VISIBLE
                    uiState.onClick(item.id)
                } else {
                    binding.sendInvitationChose.visibility = View.GONE
                    uiState.onClickRemove(item.id)
                }
            }
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
        holder.bind(friend, uiState)
    }
}