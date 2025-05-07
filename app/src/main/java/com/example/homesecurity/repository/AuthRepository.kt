package com.example.homesecurity.repository

import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
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
} 