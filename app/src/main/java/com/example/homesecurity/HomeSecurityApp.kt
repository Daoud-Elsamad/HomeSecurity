package com.example.homesecurity

import android.app.Application
import com.example.homesecurity.services.NotificationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HomeSecurityApp : Application() {
    @Inject
    lateinit var notificationService: NotificationService
    
    override fun onCreate() {
        super.onCreate()
        notificationService.initialize()
    }
}

//done//