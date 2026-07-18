/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import com.mesh.emergency.feature.emergency.domain.SosState
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.presentation.ChatUiState
import com.mesh.emergency.feature.message.presentation.MessageListUiState
import com.mesh.emergency.feature.message.presentation.deliveryStatusLabel
import com.mesh.emergency.feature.emergency.presentation.EmergencyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Emergency and Messaging feature domain models and UI state.
 * Pure JVM tests — no Android framework dependency.
 */
class EmergencyMessagingTest {

    // ── SOS State Machine ─────────────────────────────────────────────────────

    @Test
    fun sosState_initialState_isReady() {
        val state = EmergencyUiState()
        assertEquals(SosState.READY, state.sosState)
    }

    @Test
    fun sosState_allCasesExist() {
        val states = SosState.entries
        assertTrue(states.contains(SosState.READY))
        assertTrue(states.contains(SosState.CONFIRMING))
        assertTrue(states.contains(SosState.ACTIVE))
        assertTrue(states.contains(SosState.ACKNOWLEDGED))
        assertTrue(states.contains(SosState.RESOLVED))
    }

    @Test
    fun emergencyEvent_resolved_isResolved() {
        val event = makeEmergencyEvent(isResolved = true, status = DbEmergencyStatus.RESOLVED)
        assertTrue(event.isResolved)
        assertEquals(DbEmergencyStatus.RESOLVED, event.status)
    }

    @Test
    fun emergencyEvent_active_isNotResolved() {
        val event = makeEmergencyEvent(isResolved = false, status = DbEmergencyStatus.BROADCASTING)
        assertFalse(event.isResolved)
    }

    @Test
    fun emergencyUiState_activeAndHistory_arePartitioned() {
        val events = listOf(
            makeEmergencyEvent("e1", isResolved = false),
            makeEmergencyEvent("e2", isResolved = true),
            makeEmergencyEvent("e3", isResolved = false)
        )
        val active  = events.filter { !it.isResolved }
        val history = events.filter { it.isResolved }
        assertEquals(2, active.size)
        assertEquals(1, history.size)
    }

    @Test
    fun emergencyUiState_selection_roundtrip() {
        val event = makeEmergencyEvent("sel-01")
        val state = EmergencyUiState(selectedEvent = event)
        assertNotNull(state.selectedEvent)
        assertEquals("sel-01", state.selectedEvent?.id)

        val cleared = state.copy(selectedEvent = null)
        assertNull(cleared.selectedEvent)
    }

    // ── Delivery Status Labels ────────────────────────────────────────────────

    @Test
    fun deliveryStatus_pending_hasClockLabel() {
        val label = deliveryStatusLabel(DbDeliveryStatus.PENDING)
        assertTrue(label.isNotEmpty())
    }

    @Test
    fun deliveryStatus_delivered_hasTwoCheckmarks() {
        val label = deliveryStatusLabel(DbDeliveryStatus.DELIVERED)
        assertEquals("✓✓", label)
    }

    @Test
    fun deliveryStatus_failed_hasXLabel() {
        val label = deliveryStatusLabel(DbDeliveryStatus.FAILED)
        assertEquals("✗", label)
    }

    @Test
    fun deliveryStatus_queued_hasBoxLabel() {
        val label = deliveryStatusLabel(DbDeliveryStatus.QUEUED)
        assertEquals("📦", label)
    }

    // ── Message Domain Model ──────────────────────────────────────────────────

    @Test
    fun message_isSelf_trueWhenSenderIsSelf() {
        val msg = makeMessage(senderId = "self")
        assertTrue(msg.isSelf)
    }

    @Test
    fun message_isSelf_falseWhenSenderIsOther() {
        val msg = makeMessage(senderId = "node-alpha")
        assertFalse(msg.isSelf)
    }

    @Test
    fun message_retryCount_defaultIsZero() {
        val msg = makeMessage()
        assertEquals(0, msg.retryCount)
    }

    @Test
    fun message_priority_criticalIsHighest() {
        val priorities = listOf(
            DbMessagePriority.LOW,
            DbMessagePriority.MEDIUM,
            DbMessagePriority.HIGH,
            DbMessagePriority.CRITICAL
        )
        assertEquals(DbMessagePriority.CRITICAL, priorities.last())
    }

    // ── ChatUiState ───────────────────────────────────────────────────────────

    @Test
    fun chatUiState_pendingCount_reflectsQueuedMessages() {
        val messages = listOf(
            makeMessage(status = DbDeliveryStatus.QUEUED),
            makeMessage(status = DbDeliveryStatus.QUEUED),
            makeMessage(status = DbDeliveryStatus.DELIVERED)
        )
        val pendingCount = messages.count {
            it.deliveryStatus in listOf(DbDeliveryStatus.PENDING, DbDeliveryStatus.QUEUED)
        }
        assertEquals(2, pendingCount)
    }

    @Test
    fun chatUiState_draftText_defaultIsEmpty() {
        val state = ChatUiState()
        assertTrue(state.draftText.isEmpty())
    }

    @Test
    fun chatUiState_isOnline_defaultIsFalse() {
        val state = ChatUiState()
        assertFalse(state.isOnline)
    }

    // ── MessageListUiState ────────────────────────────────────────────────────

    @Test
    fun messageListUiState_initialIsLoading() {
        val state = MessageListUiState()
        assertTrue(state.isLoading)
    }

    @Test
    fun messageListUiState_conversations_defaultEmpty() {
        val state = MessageListUiState()
        assertTrue(state.conversations.isEmpty())
    }

    @Test
    fun conversationSummary_unreadCount_isCorrect() {
        val conv = ConversationSummary(
            id = "c1", title = "Node Alpha",
            lastMessagePreview = "Hello", unreadCount = 3,
            updatedAt = System.currentTimeMillis()
        )
        assertEquals(3, conv.unreadCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeEmergencyEvent(
        id: String = "test-event-001",
        isResolved: Boolean = false,
        status: DbEmergencyStatus = DbEmergencyStatus.BROADCASTING
    ) = EmergencyEvent(
        id        = id,
        type      = DbEmergencyType.SOS,
        priority  = DbMessagePriority.CRITICAL,
        status    = status,
        senderId  = "self",
        message   = "Test SOS",
        latitude  = 23.8103,
        longitude = 90.4125,
        timestamp = System.currentTimeMillis(),
        isResolved = isResolved,
        ttl       = System.currentTimeMillis() + 3_600_000L
    )

    private fun makeMessage(
        senderId: String = "self",
        status: DbDeliveryStatus = DbDeliveryStatus.SENT
    ) = Message(
        id             = "msg-test-001",
        conversationId = "conv-test",
        senderId       = senderId,
        recipientId    = "node-alpha",
        content        = "Test message",
        timestamp      = System.currentTimeMillis(),
        deliveryStatus = status,
        type           = DbMessageType.TEXT,
        priority       = DbMessagePriority.MEDIUM,
        retryCount     = 0,
        expiryTime     = System.currentTimeMillis() + 86_400_000L
    )
}
