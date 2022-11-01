package com.mindyhsu.minmap.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.databinding.ItemChatRoomBinding
import com.mindyhsu.minmap.databinding.ItemDialogBinding

class DialogAdapter() :
    ListAdapter<Message, DialogAdapter.DialogViewHolder>(DialogDiffCallback()) {

    class DialogViewHolder(private var binding: ItemDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Message) {
            binding.dialogText.text = item.text
        }
    }

    class DialogDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogViewHolder {
        return DialogViewHolder(
            ItemDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DialogViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
    }
}