package com.mindyhsu.minmap.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.bindImage
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.databinding.ItemChatRoomBinding
import timber.log.Timber

class ChatRoomAdapter(private val uiState: ChatRoomUiState) :
    ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    class ChatRoomViewHolder(private var binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatRoom, uiState: ChatRoomUiState) {
            val participants = uiState.roomTitleDisplay(item.participants)
            binding.friendName.text = participants
            binding.lastMessage.text = uiState.roomMessageDisplay(item.lastMessage)
            bindImage(binding.friendPic, uiState.roomPicDisplay(item.users))

            if (item.eventId.isNotEmpty()) {
                binding.eventReminder.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                uiState.onClick(item.id)
            }
        }
    }

    class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        return ChatRoomViewHolder(
            ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chatRoom = getItem(position)
        holder.bind(chatRoom, uiState)
    }
}