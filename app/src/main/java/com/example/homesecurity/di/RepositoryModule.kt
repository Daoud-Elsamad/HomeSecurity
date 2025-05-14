package com.example.homesecurity.di

import com.example.homesecurity.repository.AuthRepository
import com.example.homesecurity.repository.FirebaseAuthRepository
import com.example.homesecurity.repository.SensorRepository
import com.example.homesecurity.repository.NfcRepository
import com.example.homesecurity.repository.SettingsRepository
import com.example.homesecurity.repository.SettingsRepositoryImpl
import com.example.homesecurity.repository.FirestoreNfcRepository
import com.example.homesecurity.repository.HybridSensorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        hybridSensorRepository: HybridSensorRepository
    ): SensorRepository

    @Binds
    @Singleton
    abstract fun bindNfcRepository(
        firestoreNfcRepository: FirestoreNfcRepository
    ): NfcRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository
} 