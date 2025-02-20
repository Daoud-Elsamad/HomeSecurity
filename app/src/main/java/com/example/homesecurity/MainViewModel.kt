package com.example.homesecurity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    
    private val _systemArmed = MutableLiveData<Boolean>()
    val systemArmed: LiveData<Boolean> = _systemArmed

    init {
        // Initialize with disarmed state
        _systemArmed.value = false
    }

    fun toggleSystem() {
        val currentState = _systemArmed.value ?: false
        _systemArmed.postValue(!currentState)
    }
} 