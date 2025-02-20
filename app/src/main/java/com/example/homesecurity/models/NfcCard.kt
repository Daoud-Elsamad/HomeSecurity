package com.example.homesecurity.models

import java.util.UUID

data class NfcCard(
    val id: String,
    val userId: String,
    val accessLevel: AccessLevel,
    val isActive: Boolean = true,
    val registeredAt: Long = System.currentTimeMillis()
)

enum class AccessLevel {
    ADMIN,
    RESIDENT,
    GUEST
}

data class AccessLog(
    val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGranted: Boolean,
    val doorId: String,
    val userId: String
) 