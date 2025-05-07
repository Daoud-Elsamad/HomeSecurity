package com.example.homesecurity.repository

import android.util.Log
import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor() : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val systemConfigCollection = db.collection("system_config")

    private val DEFAULT_ADMIN_USERNAME = "admin"
    private val DEFAULT_ADMIN_PASSWORD = "homesecurity123"
    private val SYSTEM_CONFIG_DOC_ID = "initialization"

    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val allUsersFlow = MutableStateFlow<List<User>>(emptyList())
    

    init {
        // Initialize current user when repository is created
        refreshCurrentUser()
        // Start listening for users
        listenForUsers()
    }

    private fun refreshCurrentUser() {
        auth.currentUser?.let { firebaseUser ->
            getUserFromFirestore(firebaseUser.uid)
        } ?: run {
            currentUserFlow.value = null
        }
    }

    private fun getUserFromFirestore(userId: String) {
        usersCollection.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    currentUserFlow.value = user
                } else {
                    currentUserFlow.value = null
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user document: ", exception)
                currentUserFlow.value = null
            }
    }

    private fun listenForUsers() {
        usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for users: ", error)
                return@addSnapshotListener
            }

            val usersList = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user: ", e)
                    null
                }
            } ?: emptyList()

            allUsersFlow.value = usersList
        }
    }

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            // For the first login with default admin credentials
            if (isDefaultAdminCredentials(username, password) && !isSystemInitialized()) {
                createDefaultAdminAccount(username, password)
            } else {
                // Normal login flow
                val email = "$username@homesecurity.app" // Create email from username
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(Exception("Authentication failed"))

                val userDoc = usersCollection.document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                        ?: return Result.failure(Exception("Failed to parse user data"))
                    currentUserFlow.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("User document not found"))
                }
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Invalid username"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid password"))
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ", e)
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    override suspend fun logout() {
        auth.signOut()
        currentUserFlow.value = null
    }

    override suspend fun isSystemInitialized(): Boolean {
        return try {
            val configDoc = systemConfigCollection.document(SYSTEM_CONFIG_DOC_ID).get().await()
            configDoc.exists() && configDoc.getBoolean("initialized") == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking system initialization: ", e)
            false
        }
    }

    override suspend fun getCurrentUser(): User? {
        return currentUserFlow.value
    }

    override suspend fun isDefaultAdminCredentials(username: String, password: String): Boolean {
        return username == DEFAULT_ADMIN_USERNAME && password == DEFAULT_ADMIN_PASSWORD
    }

    override suspend fun isFirstLogin(): Boolean {
        val user = getCurrentUser() ?: return false
        return user.role == UserRole.ADMIN && !user.hasChangedDefaultPassword
    }

    override suspend fun updateDefaultAdminPassword(newPassword: String): Result<Unit> {
        return try {
            // Update Firebase Auth password
            auth.currentUser?.updatePassword(newPassword)?.await()

            // Update the user document in Firestore
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            usersCollection.document(userId)
                .update("hasChangedDefaultPassword", true)
                .await()

            // Update local user object
            currentUserFlow.value = currentUserFlow.value?.copy(hasChangedDefaultPassword = true)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password: ", e)
            Result.failure(e)
        }
    }

    override suspend fun createUser(username: String, password: String, isAdmin: Boolean): Result<User> {
        return try {
            val email = "$username@homesecurity.app" // Create email from username
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("User creation failed"))

            val role = if (isAdmin) UserRole.ADMIN else UserRole.GUEST
            val permissions = UserPermissions.fromRole(role)

            val user = User(
                id = firebaseUser.uid,
                username = username,
                email = email,
                role = role,
                permissions = permissions,
                hasChangedDefaultPassword = true // Normal users start with changed password
            )

            // Save user to Firestore
            usersCollection.document(firebaseUser.uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user: ", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserPermissions(userId: String, permissions: UserPermissions): Result<Unit> {
        return try {
            // Make sure current user is admin
            val currentUser = currentUserFlow.value
            if (currentUser?.role != UserRole.ADMIN) {
                return Result.failure(Exception("Only admins can update user permissions"))
            }
            
            // Update the permissions in Firestore
            usersCollection.document(userId)
                .update("permissions", permissions)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user permissions: ", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllUsers(): Flow<List<User>> = allUsersFlow

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                getUserFromFirestore(firebaseUser.uid)
            } else {
                currentUserFlow.value = null
                trySend(null)
            }
        }

        // Send the current value immediately
        trySend(currentUserFlow.value)

        // Set up the listener
        auth.addAuthStateListener(authStateListener)

        // Set up the current user flow collection
        val job = currentUserFlow.collect {
            trySend(it)
        }

        // Clean up when flow collection stops
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    private suspend fun createDefaultAdminAccount(username: String, password: String): Result<User> {
        return try {
            // Create the admin user in Firebase Auth
            val email = "$username@homesecurity.app"
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Admin creation failed"))

            // Create the admin user document in Firestore
            val adminUser = User(
                id = firebaseUser.uid,
                username = username,
                email = email,
                role = UserRole.ADMIN,
                permissions = UserPermissions.fromRole(UserRole.ADMIN),
                hasChangedDefaultPassword = false // Require password change on first login
            )

            // Save user to Firestore
            usersCollection.document(firebaseUser.uid).set(adminUser).await()

            // Mark system as initialized
            systemConfigCollection.document(SYSTEM_CONFIG_DOC_ID)
                .set(mapOf("initialized" to true))
                .await()

            currentUserFlow.value = adminUser
            Result.success(adminUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default admin account: ", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthRepo"
    }
} 