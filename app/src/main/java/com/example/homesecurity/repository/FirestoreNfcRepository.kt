package com.example.homesecurity.repository

import android.util.Log
import com.example.homesecurity.models.AccessLevel
import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.NfcCard
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNfcRepository @Inject constructor() : NfcRepository {
    private val db = FirebaseFirestore.getInstance()
    private val accessAttemptsCollection = db.collection("AccessAttempts")
    private val nfcCardsCollection = db.collection("NFCCards")
    
    private val _accessLogs = MutableStateFlow<List<AccessLog>>(emptyList())
    private val _registeredCards = MutableStateFlow<List<NfcCard>>(emptyList())
    
    init {
        // Listen for access attempts
        accessAttemptsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed for access attempts", error)
                return@addSnapshotListener
            }
            
            val logs = snapshot?.documents?.mapNotNull { doc ->
                try {
                    AccessLog(
                        id = doc.id,
                        cardId = doc.getString("reader_id") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time 
                            ?: System.currentTimeMillis(),
                        isGranted = doc.getBoolean("success") ?: false,
                        doorId = doc.getString("system_id") ?: "",
                        userId = doc.getString("USER_ID_456") ?: "unknown"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing access log", e)
                    null
                }
            } ?: emptyList()
            
            _accessLogs.value = logs
        }
        
        // Listen for NFC cards
        nfcCardsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed for NFC cards", error)
                return@addSnapshotListener
            }
            
            val cards = snapshot?.documents?.mapNotNull { doc ->
                try {
                    NfcCard(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        accessLevel = doc.getString("accessLevel")?.let { 
                            AccessLevel.valueOf(it) 
                        } ?: AccessLevel.GUEST,
                        isActive = doc.getBoolean("isActive") ?: true,
                        registeredAt = doc.getTimestamp("registeredAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing NFC card", e)
                    null
                }
            } ?: emptyList()
            
            _registeredCards.value = cards
        }
    }

    override suspend fun registerCard(card: NfcCard): Boolean = try {
        nfcCardsCollection.document(card.id).set(
            hashMapOf(
                "userId" to card.userId,
                "accessLevel" to card.accessLevel.name,
                "isActive" to card.isActive,
                "registeredAt" to FieldValue.serverTimestamp()
            )
        ).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error registering card", e)
        false
    }

    override suspend fun validateAccess(cardId: String, doorId: String): Boolean {
        val card = _registeredCards.value.find { it.id == cardId }
        val isGranted = card?.isActive == true
        
        try {
            accessAttemptsCollection.add(
                hashMapOf(
                    "reader_id" to cardId,
                    "success" to isGranted,
                    "system_id" to doorId,
                    "USER_ID_456" to (card?.userId ?: "unknown"),
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error logging access attempt", e)
        }
        
        return isGranted
    }

    override suspend fun getAccessLogs(): Flow<List<AccessLog>> = _accessLogs
    
    override suspend fun getRegisteredCards(): Flow<List<NfcCard>> = _registeredCards

    override suspend fun deactivateCard(cardId: String) {
        try {
            nfcCardsCollection.document(cardId)
                .update("isActive", false)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating card", e)
        }
    }

    override suspend fun getCurrentUser(): NfcCard? {
        // For testing, return a mock admin user
        return NfcCard(
            id = "admin_card",
            userId = "admin",
            accessLevel = AccessLevel.ADMIN
        )
    }

    companion object {
        private const val TAG = "FirestoreNfcRepo"
    }
} 