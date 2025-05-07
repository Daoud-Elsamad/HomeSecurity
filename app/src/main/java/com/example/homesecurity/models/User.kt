package com.example.homesecurity.models

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val username: String = "",
    val email: String = "",
    val role: UserRole = UserRole.GUEST,
    val enabled: Boolean = true,
    val hasChangedDefaultPassword: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val permissions: UserPermissions = UserPermissions()
) {
    constructor() : this(username = "")
}

enum class UserRole {
    ADMIN,
    RESIDENT,
    GUEST
}

data class UserPermissions(
    val canViewLogs: Boolean = false,
    val canManageSensors: Boolean = false,
    val canManageUsers: Boolean = false
) {
    constructor() : this(false, false, false)
    
    companion object {
        fun fromRole(role: UserRole): UserPermissions = when (role) {
            UserRole.ADMIN -> UserPermissions(
                canViewLogs = true,
                canManageSensors = true,
                canManageUsers = true
            )
            UserRole.RESIDENT -> UserPermissions(
                canViewLogs = true,
                canManageSensors = true,
                canManageUsers = false
            )
            UserRole.GUEST -> UserPermissions(
                canViewLogs = false,
                canManageSensors = false,
                canManageUsers = false
            )
        }
    }
} 