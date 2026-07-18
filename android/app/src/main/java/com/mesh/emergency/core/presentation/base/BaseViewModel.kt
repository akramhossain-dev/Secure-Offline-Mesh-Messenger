/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for all ViewModels, standardizing MVI (Model-View-Intent) unidirectional data flow.
 *
 * Exposes:
 * - [uiState] to hold screen-level state.
 * - [effect] to emit transient one-time effects.
 *
 * ViewModels handle UI interaction actions by implementing [onEvent].
 */
abstract class BaseViewModel<State : BaseUiState, Event : BaseUiEvent, Effect : BaseUiEffect>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    /**
     * Helper to read the current state synchronously.
     */
    protected val currentState: State
        get() = _uiState.value

    /**
     * Process an incoming UI event.
     */
    abstract fun onEvent(event: Event)

    /**
     * Safely updates the current UI state.
     */
    protected fun updateState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    /**
     * Emits a transient side-effect to the UI.
     */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
