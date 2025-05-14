package com.example.homesecurity.di

import android.content.Context
import com.example.homesecurity.utils.NfcUtils
import com.example.homesecurity.utils.NotificationHelper
import com.example.homesecurity.utils.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideNfcUtils(@ApplicationContext context: Context): NfcUtils {
        return NfcUtils(context)
    }
    
    @Provides
    @Singleton
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider {
        return CoroutineScopeProvider()
    }
    
    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }
} 