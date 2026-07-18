/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton coordinator regulating Compose navigation intents.
 * ViewModels request transitions by invoking [navigateTo] or [navigateBack].
 */
@Singleton
class AppNavigator @Inject constructor() {

    private val _navigationActions = MutableSharedFlow<NavigationAction>(extraBufferCapacity = 1)

    /** Flow of navigation intents consumed by the hosting Compose NavHost. */
    val navigationActions: SharedFlow<NavigationAction> = _navigationActions.asSharedFlow()

    /**
     * Dispatches a transition request.
     *
     * @param destination Target route path.
     * @param popUpToRoute Optional route to pop up to before navigating.
     * @param inclusive Whether the popped route is also removed.
     */
    fun navigateTo(
        destination: String,
        popUpToRoute: String? = null,
        inclusive: Boolean = false
    ) {
        _navigationActions.tryEmit(
            NavigationAction.NavigateTo(
                destination = destination,
                popUpToRoute = popUpToRoute,
                inclusive = inclusive
            )
        )
    }

    /** Dispatches a pop backstack intent. */
    fun navigateBack() {
        _navigationActions.tryEmit(NavigationAction.NavigateBack)
    }
}

/**
 * Represents structured action instructions passed to the nav host.
 */
sealed interface NavigationAction {

    /** Instruction directing NavController to navigate to a specific destination. */
    data class NavigateTo(
        val destination: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false
    ) : NavigationAction

    /** Instruction directing NavController to pop backstack. */
    data object NavigateBack : NavigationAction
}
