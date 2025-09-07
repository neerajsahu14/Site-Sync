package com.a1.sitesync.ui.viewmodel

// Holds all authentication UI state
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: String? = null,
    val uid: String? = null
)
