/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Custom Compose transition specifications for navigation screens jumps.
 */
object NavTransitions {

    private const val TRANSITION_DURATION_MS = 300

    /** Transition for entering standard routes. Slide-in from right. */
    val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION_MS)
        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
    }

    /** Transition for leaving standard routes. Slide-out to left. */
    val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(TRANSITION_DURATION_MS)
        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
    }

    /** Transition for returning back to a route. Slide-in from left. */
    val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(TRANSITION_DURATION_MS)
        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
    }

    /** Transition for leaving back out of a route. Slide-out to right. */
    val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION_MS)
        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
    }
}
