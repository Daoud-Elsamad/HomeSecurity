package com.example.homesecurity.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoroutineScopeProvider @Inject constructor() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
} 