package com.example.homesecurity.repository

import com.example.homesecurity.models.AccessLevel
import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.NfcCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface NfcRepository {
    suspend fun registerCard(card: NfcCard): Boolean
    suspend fun validateAccess(cardId: String, doorId: String): Boolean
    suspend fun getAccessLogs(): Flow<List<AccessLog>>
    suspend fun getRegisteredCards(): Flow<List<NfcCard>>
    suspend fun deactivateCard(cardId: String)
    suspend fun getCurrentUser(): NfcCard?
}

@Singleton
class MockNfcRepository @Inject constructor() : NfcRepository {
    private val registeredCards = MutableStateFlow<List<NfcCard>>(emptyList())
    private val accessLogs = MutableStateFlow<List<AccessLog>>(emptyList())

    override suspend fun registerCard(card: NfcCard): Boolean {
        registeredCards.update { cards ->
            cards + card
        }
        return true
    }

    override suspend fun validateAccess(cardId: String, doorId: String): Boolean {
        val card = registeredCards.value.find { it.id == cardId }
        val isGranted = card?.isActive == true
        
        // Log the access attempt
        val log = AccessLog(
            cardId = cardId,
            isGranted = isGranted,
            doorId = doorId,
            userId = card?.userId ?: "unknown"
        )
        accessLogs.update { logs -> logs + log }
        
        return isGranted
    }

    override suspend fun getAccessLogs(): Flow<List<AccessLog>> = accessLogs

    override suspend fun getRegisteredCards(): Flow<List<NfcCard>> = registeredCards

    override suspend fun deactivateCard(cardId: String) {
        registeredCards.update { cards ->
            cards.map { 
                if (it.id == cardId) it.copy(isActive = false) else it 
            }
        }
    }

    override suspend fun getCurrentUser(): NfcCard? {
        return registeredCards.value.find { it.accessLevel == AccessLevel.ADMIN }
    }
} 