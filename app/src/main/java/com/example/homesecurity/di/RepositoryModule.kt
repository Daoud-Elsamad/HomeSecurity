package com.example.homesecurity.di

import com.example.homesecurity.repository.MockSensorRepository
import com.example.homesecurity.repository.SensorRepository
import com.example.homesecurity.repository.MockNfcRepository
import com.example.homesecurity.repository.NfcRepository
import com.example.homesecurity.repository.SettingsRepository
import com.example.homesecurity.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        mockSensorRepository: MockSensorRepository
    ): SensorRepository

    @Binds
    @Singleton
    abstract fun bindNfcRepository(
        mockNfcRepository: MockNfcRepository
    ): NfcRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
} 