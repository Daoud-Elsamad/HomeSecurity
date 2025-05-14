package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserRole

class UserAdapter(
    private val onUserSelected: (User) -> Unit = {},
    private val onUserLongPressed: (User, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    
    private var users: List<User> = emptyList()
    private var selectedUserId: String? = null
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val adminBadge: TextView = view.findViewById(R.id.adminBadge)
        val userItemContainer: View = view.findViewById(R.id.userItemContainer)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.usernameText.text = user.username
        
        // Show role badge
        when (user.role) {
            UserRole.ADMIN -> {
                holder.adminBadge.visibility = View.VISIBLE
                holder.adminBadge.text = "Admin"
                holder.adminBadge.setBackgroundResource(R.drawable.badge_admin)
            }
            UserRole.RESIDENT -> {
                holder.adminBadge.visibility = View.VISIBLE
                holder.adminBadge.text = "Resident"
                holder.adminBadge.setBackgroundResource(R.drawable.badge_resident)
            }
            UserRole.GUEST -> {
                holder.adminBadge.visibility = View.VISIBLE
                holder.adminBadge.text = "Guest"
                holder.adminBadge.setBackgroundResource(R.drawable.badge_guest)
            }
        }
        
        // Handle selection
        if (user.id == selectedUserId) {
            holder.userItemContainer.setBackgroundResource(R.drawable.selected_item_background)
        } else {
            holder.userItemContainer.setBackgroundResource(R.drawable.default_item_background)
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener {
            onUserSelected(user)
            selectUser(user.id)
        }
        
        holder.itemView.setOnLongClickListener {
            onUserLongPressed(user, holder.itemView)
            true
        }
    }
    
    override fun getItemCount() = users.size
    
    fun updateUsers(newUsers: List<User>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = users.size
            override fun getNewListSize() = newUsers.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return users[oldItemPosition].id == newUsers[newItemPosition].id
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return users[oldItemPosition] == newUsers[newItemPosition]
            }
        })
        
        users = newUsers
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun selectUser(userId: String?) {
        val previouslySelectedId = selectedUserId
        selectedUserId = userId
        
        // Update previous selection
        if (previouslySelectedId != null) {
            val previousIndex = users.indexOfFirst { it.id == previouslySelectedId }
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex)
            }
        }
        
        // Update new selection
        if (userId != null) {
            val newIndex = users.indexOfFirst { it.id == userId }
            if (newIndex >= 0) {
                notifyItemChanged(newIndex)
            }
        }
    }
    
    fun getSelectedUser(): User? {
        return users.find { it.id == selectedUserId }
    }
} 