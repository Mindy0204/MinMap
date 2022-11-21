package com.mindyhsu.minmap.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.data.User
import com.mindyhsu.minmap.databinding.ItemFriendLocationBinding

class FriendLocationAdapter(private val uiState: MapUiState) :
    ListAdapter<User, FriendLocationAdapter.FriendLocationHolder>(FriendLocationDiffCallback()) {

    class FriendLocationHolder(private var binding: ItemFriendLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: User, uiState: MapUiState) {
            binding.friendImage
            binding.friendNameText.text = item.name
            itemView.setOnClickListener {
                uiState.onClick(item.id)
            }
        }
    }

    class FriendLocationDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendLocationHolder {
        return FriendLocationHolder(
            ItemFriendLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FriendLocationHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, uiState)
    }
}