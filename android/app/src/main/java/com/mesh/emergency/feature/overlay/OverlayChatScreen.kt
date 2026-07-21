/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colour constants matching Messenger dark theme ─────────────────────────────
private val MessengerBlue  = Color(0xFF0084FF)
private val PopupBg        = Color(0xFF1C1E22)
private val HeaderBg       = Color(0xFF252729)
private val IncomingBubble = Color(0xFF3A3B3C)
private val InputBg        = Color(0xFF2C2D30)

// ── Root Popup Screen ─────────────────────────────────────────────────────────

@Composable
fun OverlayChatScreen(
    viewModel: OverlayConversationViewModel,
    onClose: () -> Unit,
    onExpand: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val messages = state.messages

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Surface(
        color          = PopupBg,
        shape          = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp,
                                            bottomStart = 16.dp, bottomEnd = 16.dp),
        shadowElevation = 24.dp,
        modifier       = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────────
            OverlayHeader(
                peerName = state.peerName,
                isOnline = state.isOnline,
                onClose  = onClose,
                onExpand = onExpand
            )

            // ── Message List ──────────────────────────────────────────────────
            LazyColumn(
                state               = listState,
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                contentPadding      = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = messages,
                    key   = { _, msg -> msg.id.ifBlank { "msg_${msg.hashCode()}" } }
                ) { index, msg ->
                    val prevMsg = messages.getOrNull(index - 1)
                    val nextMsg = messages.getOrNull(index + 1)

                    // Grouping logic: same sender within 2 minutes = same group
                    val isFirstInGroup = prevMsg == null
                        || prevMsg.isSelf != msg.isSelf
                        || prevMsg.senderName != msg.senderName
                        || (msg.timestamp - prevMsg.timestamp) > 120_000L

                    val isLastInGroup = nextMsg == null
                        || nextMsg.isSelf != msg.isSelf
                        || nextMsg.senderName != msg.senderName
                        || (nextMsg.timestamp - msg.timestamp) > 120_000L

                    // Spacing: larger gap between groups, tiny within group
                    if (isFirstInGroup && index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OverlayMessageBubble(
                        msg           = msg,
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup  = isLastInGroup
                    )
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
            )

            // ── Input Bar ─────────────────────────────────────────────────────
            OverlayInputBar(
                draft     = state.draftText,
                isSending = state.isSending,
                onDraft   = viewModel::updateDraft,
                onSend    = viewModel::sendMessage
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun OverlayHeader(
    peerName: String,
    isOnline: Boolean,
    onClose:  () -> Unit,
    onExpand: () -> Unit
) {
    val onlineColor by animateColorAsState(
        targetValue  = if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
        animationSpec = tween(500),
        label        = "online_color"
    )

    Surface(
        color = HeaderBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(
                        colors = listOf(MessengerBlue, Color(0xFF0055CC))
                    )),
                contentAlignment = Alignment.Center
            ) {
                val initials = if (peerName.startsWith("Global")) "🌐"
                               else peerName.take(2).uppercase()
                Text(
                    text  = initials,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = peerName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    ),
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier
                        .size(7.dp).clip(CircleShape).background(onlineColor)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text  = if (isOnline) "Mesh Connected" else "Offline Queue",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = onlineColor
                    )
                }
            }

            IconButton(onClick = onExpand, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Full Chat",
                    tint               = Color.White.copy(alpha = 0.7f),
                    modifier           = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Close",
                    tint               = Color.White.copy(alpha = 0.7f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Message Bubble ─────────────────────────────────────────────────────────────

@Composable
private fun OverlayMessageBubble(
    msg: OverlayMessage,
    isFirstInGroup: Boolean,
    isLastInGroup:  Boolean
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isSelf = msg.isSelf

    // Bubble corner radii — Messenger style:
    // Outer corners always round, inner corners sharp within a group
    val bigR  = 18.dp
    val smallR = 4.dp
    val shape = RoundedCornerShape(
        topStart    = if (isSelf)  bigR else if (isFirstInGroup) bigR else smallR,
        topEnd      = if (isSelf)  if (isFirstInGroup) bigR else smallR else bigR,
        bottomStart = if (isSelf)  bigR else if (isLastInGroup)  bigR else smallR,
        bottomEnd   = if (isSelf)  if (isLastInGroup)  bigR else smallR else bigR
    )

    val bubbleBg = if (isSelf)
        Brush.horizontalGradient(listOf(MessengerBlue, Color(0xFF0069D9)))
    else
        Brush.horizontalGradient(listOf(IncomingBubble, IncomingBubble))

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {
        // Sender name — only at top of group, incoming messages only
        if (!isSelf && isFirstInGroup && msg.senderName.isNotBlank()) {
            Text(
                text     = msg.senderName,
                style    = MaterialTheme.typography.bodySmall.copy(
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MessengerBlue
                ),
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(shape)
                .background(brush = bubbleBg)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Column {
                Text(
                    text  = msg.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color    = Color.White,
                        fontSize = 13.sp
                    )
                )
                // Time only at the bottom of each group
                if (isLastInGroup) {
                    Text(
                        text     = timeFormatter.format(Date(msg.timestamp)),
                        style    = MaterialTheme.typography.bodySmall.copy(
                            color    = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

// ── Input Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun OverlayInputBar(
    draft:    String,
    isSending: Boolean,
    onDraft:  (String) -> Unit,
    onSend:   () -> Unit
) {
    val canSend = draft.isNotBlank() && !isSending
    val sendScale by animateFloatAsState(
        targetValue   = if (canSend) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "send_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "😊", fontSize = 22.sp, modifier = Modifier.padding(end = 6.dp))

        TextField(
            value         = draft,
            onValueChange = onDraft,
            placeholder   = {
                Text("Aa", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            },
            modifier      = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = InputBg,
                unfocusedContainerColor = InputBg,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = MessengerBlue
            ),
            textStyle       = MaterialTheme.typography.bodyMedium.copy(
                color    = Color.White,
                fontSize = 14.sp
            ),
            maxLines        = 4,
            singleLine      = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(sendScale)
                .clip(CircleShape)
                .background(if (canSend) MessengerBlue else Color(0xFF3A3B3C)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { if (canSend) onSend() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}
