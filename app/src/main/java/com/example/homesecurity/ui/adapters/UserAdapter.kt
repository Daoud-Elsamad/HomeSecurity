package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserRole

class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    
    private var users: List<User> = emptyList()
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val adminBadge: TextView = view.findViewById(R.id.adminBadge)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.usernameText.text = user.username
        
        // Show admin badge only for admins
        if (user.role == UserRole.ADMIN) {
            holder.adminBadge.visibility = View.VISIBLE
        } else {
            holder.adminBadge.visibility = View.GONE
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
} 