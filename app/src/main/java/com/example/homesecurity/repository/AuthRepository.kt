package com.example.homesecurity.repository

import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.models.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
    suspend fun isSystemInitialized(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun isDefaultAdminCredentials(username: String, password: String): Boolean
    suspend fun isFirstLogin(): Boolean
    suspend fun updateDefaultAdminPassword(newPassword: String): Result<Unit>
    suspend fun createUser(username: String, password: String, isAdmin: Boolean): Result<User>
    suspend fun updateUserPermissions(userId: String, permissions: UserPermissions): Result<Unit>
    suspend fun getAllUsers(): Flow<List<User>>
    fun observeAuthState(): Flow<User?>
    
    // New methods
    suspend fun getUserById(userId: String): Result<User>
    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun createUserWithRole(username: String, password: String, role: UserRole): Result<User>
    
    // Method to force refresh users list
    fun refreshUsersList()
} 