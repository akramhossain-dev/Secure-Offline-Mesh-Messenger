/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.UserDomainModel
import com.mesh.emergency.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI-based ViewModel managing state for user profile customization.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileUiEffect>()
    val effect: SharedFlow<ProfileUiEffect> = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                usernameInput = result.data.username,
                                userModel = result.data
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onUsernameChanged(input: String) {
        _uiState.update { it.copy(usernameInput = input) }
    }

    fun saveProfile() {
        val currentInput = _uiState.value.usernameInput.trim()
        if (currentInput.isEmpty()) {
            viewModelScope.launch {
                _effect.emit(ProfileUiEffect.ShowToast("Nickname cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = userRepository.updateProfile(currentInput, null)
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Result.Success -> {
                    _effect.emit(ProfileUiEffect.ShowToast("Profile updated successfully"))
                    _effect.emit(ProfileUiEffect.SaveSuccess)
                }
                is Result.Error   -> _effect.emit(ProfileUiEffect.ShowToast("Update failed: ${result.exception.message}"))
                else -> Unit
            }
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val usernameInput: String = "",
    val userModel: UserDomainModel? = null,
    val errorMessage: String? = null
)

sealed interface ProfileUiEffect {
    data class ShowToast(val message: String) : ProfileUiEffect
    data object SaveSuccess : ProfileUiEffect
}
