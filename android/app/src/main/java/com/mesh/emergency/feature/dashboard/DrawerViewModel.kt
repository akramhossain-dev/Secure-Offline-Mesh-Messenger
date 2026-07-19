/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.domain.AppState
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.domain.repository.UserDomainModel
import com.mesh.emergency.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the responsive navigation drawer.
 * Manages user profile information and quick action settings toggles.
 */
@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appStateRepository: AppStateRepository
) : ViewModel() {

    /** Exposes the current user's profile domain state. */
    val currentUser: StateFlow<Result<UserDomainModel>> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Result.Loading
        )

    /** Exposes the global application settings state (theme, language, isOnline). */
    val appState: StateFlow<AppState> = appStateRepository.appState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppState()
        )

    /** Mutates the current application theme. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appStateRepository.setThemeMode(mode)
        }
    }

    /** Mutates the current application language. */
    fun setLanguage(code: String) {
        viewModelScope.launch {
            appStateRepository.setLanguage(code)
        }
    }
}
