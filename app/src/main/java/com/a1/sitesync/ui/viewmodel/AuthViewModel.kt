package com.a1.sitesync.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a1.sitesync.data.repository.AuthRepository
import com.a1.sitesync.data.repository.AuthResult
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onEmailChange(email: String) {
        uiState = uiState.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password)
    }

    fun onDisplayNameChange(displayName: String) {
        uiState = uiState.copy(displayName = displayName)
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        uiState = uiState.copy(phoneNumber = phoneNumber)
    }

    fun toggleSignUp() {
        uiState = uiState.copy(isSignUp = !uiState.isSignUp)
    }

    fun onAuthAction() {
        if (uiState.isSignUp) {
            signUp()
        } else {
            signIn()
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = authRepository.signIn(uiState.email, uiState.password)) {
                is AuthResult.Success -> {
                    uiState = uiState.copy(isLoading = false, isSignedIn = true, uid = result.user.uid)
                }
                is AuthResult.Error -> {
                    uiState = uiState.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = authRepository.signUp(
                email = uiState.email,
                password = uiState.password,
                displayName = uiState.displayName,
                phoneNumber = uiState.phoneNumber,
                profilePictureUrl = null // Not handled in UI yet
            )) {
                is AuthResult.Success -> {
                    uiState = uiState.copy(isLoading = false, isSignedIn = true, uid = result.user.uid)
                }
                is AuthResult.Error -> {
                    uiState = uiState.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }
}