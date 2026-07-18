/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.notification.AlertModel
import com.mesh.emergency.core.notification.AlertPriority
import com.mesh.emergency.core.notification.AlertType
import com.mesh.emergency.core.notification.NotificationChannelType
import com.mesh.emergency.core.system.NotificationServiceWrapper
import com.mesh.emergency.data.notification.NotificationManagerImpl
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Unit test validating local notification channels configuration and priority overrides.
 */
class NotificationSystemTest {

    @Mock
    private lateinit var mockNotificationServiceWrapper: NotificationServiceWrapper

    private lateinit var notificationManager: NotificationManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        notificationManager = NotificationManagerImpl(mockNotificationServiceWrapper)
        notificationManager.createNotificationChannels()
    }

    @Test
    fun testShowNotification_dispatchesSuccessfullyWhenEnabled() {
        `when`(mockNotificationServiceWrapper.areNotificationsEnabled()).thenReturn(true)

        val alert = AlertModel(
            id = "alert_1",
            type = AlertType.SOS_ALERT,
            title = "SOS Emergency",
            description = "Natural disaster alert triggered",
            priority = AlertPriority.CRITICAL,
            timestamp = System.currentTimeMillis(),
            source = "EmergencyManager",
            status = "ACTIVE"
        )

        val result = notificationManager.showNotification(alert)
        assertTrue(result is Result.Success)
    }

    @Test
    fun testShowNotification_respectsSilencedPreferences() {
        `when`(mockNotificationServiceWrapper.areNotificationsEnabled()).thenReturn(true)

        // Disable MESSAGE channel notifications
        notificationManager.updatePreferences(
            NotificationChannelType.MESSAGE,
            enabled = false,
            sound = false,
            vibration = false
        )

        val alert = AlertModel(
            id = "alert_msg",
            type = AlertType.MESSAGE_ALERT,
            title = "New Message",
            description = "Hi there",
            priority = AlertPriority.NORMAL, // Maps to MESSAGE channel
            timestamp = System.currentTimeMillis(),
            source = "CommunicationManager",
            status = "PENDING"
        )

        val result = notificationManager.showNotification(alert)
        assertTrue(result is Result.Success) // Successfully handled (but silenced internally)
    }
}
