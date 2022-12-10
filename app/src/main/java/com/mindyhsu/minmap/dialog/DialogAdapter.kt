package com.mindyhsu.minmap.dialog

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.databinding.ItemDialogDateBinding
import com.mindyhsu.minmap.databinding.ItemFriendDialogBinding
import com.mindyhsu.minmap.databinding.ItemMyDialogBinding
import java.text.SimpleDateFormat

sealed class DialogItem {
    abstract val senderId: String
    abstract val time: Timestamp

    data class DialogDate(val message: Message) : DialogItem() {
        override val senderId: String
            get() = message.senderId
        override val time: Timestamp
            get() = message.time!!
    }

    data class MyDialog(val message: Message) : DialogItem() {
        override val senderId: String
            get() = message.senderId
        override val time: Timestamp
            get() = message.time!!
    }

    data class FriendDialog(val message: Message) : DialogItem() {
        override val senderId: String
            get() = message.senderId
        override val time: Timestamp
            get() = message.time!!
    }
}

private const val ITEM_VIEW_DIALOG_DATE = 0x00
private const val ITEM_VIEW_MY_DIALOG = 0x01
private const val ITEM_VIEW_FRIEND_DIALOG = 0x02

@SuppressLint("SimpleDateFormat")
class DialogAdapter(private val uiState: DialogUiState) :
    ListAdapter<DialogItem, RecyclerView.ViewHolder>(DialogDiffCallback()) {

    class DialogDateViewHolder(private var binding: ItemDialogDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DialogItem.DialogDate, uiStatue: DialogUiState) {
            val dateFormat = SimpleDateFormat(DATE_FORMAT)
            binding.dialogDateText.text = dateFormat.format(item.time.toDate())
        }
    }

    class MyDialogViewHolder(private var binding: ItemMyDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DialogItem.MyDialog, uiStatue: DialogUiState) {

            // If text is a URL -> draw under line
            if (uiStatue.showUrlUnderLine(item.message.text)) {
                binding.dialogText.paint.flags = Paint.UNDERLINE_TEXT_FLAG
                binding.dialogText.setOnClickListener {

                    uiStatue.clickUrl(item.message.text)
                }
            }
            binding.dialogText.text = item.message.text

            val dateFormat = SimpleDateFormat(TIME_FORMAT)
            binding.dialogTimeText.text = dateFormat.format(item.time.toDate())
        }
    }

    class FriendDialogViewHolder(private var binding: ItemFriendDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DialogItem.FriendDialog, uiStatue: DialogUiState) {
            binding.dialogNameText.text = uiStatue.getSenderName(item.senderId)

            // If text is a URL -> draw under line
            if (uiStatue.showUrlUnderLine(item.message.text)) {
                binding.dialogText.paint.flags = Paint.UNDERLINE_TEXT_FLAG
                binding.dialogText.setOnClickListener {
                    uiStatue.clickUrl(item.message.text)
                }
            }
            binding.dialogText.text = item.message.text

            val dateFormat = SimpleDateFormat(TIME_FORMAT)
            binding.dialogTimeText.text = dateFormat.format(item.time.toDate())
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DialogItem.MyDialog -> ITEM_VIEW_MY_DIALOG
            is DialogItem.FriendDialog -> ITEM_VIEW_FRIEND_DIALOG
            is DialogItem.DialogDate -> ITEM_VIEW_DIALOG_DATE
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
                val view =
                    ItemMyDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MyDialogViewHolder(view)
            }
            ITEM_VIEW_FRIEND_DIALOG -> {
                val view = ItemFriendDialogBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FriendDialogViewHolder(view)
            }
            else -> {
                val view = ItemDialogDateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DialogDateViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyDialogViewHolder -> {
                val item = getItem(position) as DialogItem.MyDialog
                holder.bind(item, uiState)
            }
            is FriendDialogViewHolder -> {
                val item = getItem(position) as DialogItem.FriendDialog
                holder.bind(item, uiState)
            }
            is DialogDateViewHolder -> {
                val item = getItem(position) as DialogItem.DialogDate
                holder.bind(item, uiState)
            }
        }
    }
}
