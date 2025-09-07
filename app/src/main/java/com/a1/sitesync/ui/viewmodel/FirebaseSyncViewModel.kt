package com.a1.sitesync.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.a1.sitesync.data.repository.SiteSyncRepository
import org.koin.java.KoinJavaComponent.inject
import java.util.Date

class FirebaseSyncViewModel(
    private val repository: SiteSyncRepository
) : ViewModel() {
    // Indicates if a sync is in progress
    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress

    // Timestamp of the last completed sync
    private val _lastSyncTime = MutableStateFlow<Date?>(null)
    val lastSyncTime: StateFlow<Date?> = _lastSyncTime

    // Trigger a full bi-directional sync
    fun performSync() = viewModelScope.launch {
        _syncInProgress.value = true
        repository.performDataSync()
        _lastSyncTime.value = Date()
        _syncInProgress.value = false
    }
}