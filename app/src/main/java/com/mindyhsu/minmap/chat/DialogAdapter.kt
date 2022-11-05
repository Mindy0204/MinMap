package com.mindyhsu.minmap.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.databinding.ItemFriendDialogBinding
import com.mindyhsu.minmap.databinding.ItemMyDialogBinding

sealed class DialogItem {
    abstract val senderId: String

    data class MyDialog(val message: Message): DialogItem() {
        override val senderId: String
            get() = message.senderId
    }

    data class FriendDialog(val message: Message): DialogItem() {
        override val senderId: String
            get() = message.senderId
    }
}

private const val ITEM_VIEW_MY_DIALOG = 0x00
private const val ITEM_VIEW_FRIEND_DIALOG = 0x01

class DialogAdapter :
    ListAdapter<DialogItem, RecyclerView.ViewHolder>(DialogDiffCallback()) {

    class MyDialogViewHolder(private var binding: ItemMyDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DialogItem.MyDialog) {
            binding.dialogText.text = item.message.text
        }
    }

    class FriendDialogViewHolder(private var binding: ItemFriendDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DialogItem.FriendDialog) {
            binding.dialogText.text = item.message.text
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DialogItem.MyDialog -> ITEM_VIEW_MY_DIALOG
            is DialogItem.FriendDialog -> ITEM_VIEW_FRIEND_DIALOG
        }
    }

    class DialogDiffCallback : DiffUtil.ItemCallback<DialogItem>() {
        override fun areItemsTheSame(oldItem: DialogItem, newItem: DialogItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: DialogItem, newItem: DialogItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_MY_DIALOG -> {
                val view = ItemMyDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MyDialogViewHolder(view)
            }
            else -> {
                val view = ItemFriendDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FriendDialogViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyDialogViewHolder -> {
                val item = getItem(position) as DialogItem.MyDialog
                holder.bind(item)
            }
            is FriendDialogViewHolder -> {
                val item = getItem(position) as DialogItem.FriendDialog
                holder.bind(item)
            }
        }
    }
}