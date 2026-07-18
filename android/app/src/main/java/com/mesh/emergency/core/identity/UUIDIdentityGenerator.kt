/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.identity

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IdentityGenerator] generating prefixed UUID strings offline.
 */
@Singleton
class UUIDIdentityGenerator @Inject constructor() : IdentityGenerator {

    override fun generateUserId(): String = "usr_${UUID.randomUUID()}"

    override fun generateDeviceId(): String = "dev_${UUID.randomUUID()}"

    override fun generateNodeId(): String = "nod_${UUID.randomUUID()}"

    override fun generateFingerprint(): String = UUID.randomUUID().toString().replace("-", "")
}
