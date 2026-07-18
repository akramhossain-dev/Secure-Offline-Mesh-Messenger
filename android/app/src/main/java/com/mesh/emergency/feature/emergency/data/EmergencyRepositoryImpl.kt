/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.emergency.data

import com.mesh.emergency.data.local.dao.EmergencyEventDao
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import com.mesh.emergency.feature.emergency.domain.EmergencyRepository
import com.mesh.emergency.feature.emergency.domain.toDomain
import com.mesh.emergency.feature.emergency.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [EmergencyRepository].
 * All data operations are local — no network involved.
 */
@Singleton
class EmergencyRepositoryImpl @Inject constructor(
    private val emergencyEventDao: EmergencyEventDao
) : EmergencyRepository {

    override fun getEmergencyEvents(): Flow<List<EmergencyEvent>> =
        emergencyEventDao.getEmergencyEvents().map { list -> list.map { it.toDomain() } }

    override suspend fun createEmergencyEvent(event: EmergencyEvent) {
        emergencyEventDao.insertEmergencyEvent(event.toEntity())
    }

    override suspend fun resolveEmergencyEvent(id: String) {
        val existing = emergencyEventDao.getEmergencyEventById(id) ?: return
        emergencyEventDao.insertEmergencyEvent(
            existing.copy(
                status = DbEmergencyStatus.RESOLVED,
                isResolved = true
            )
        )
    }

    override suspend fun acknowledgeEmergencyEvent(id: String) {
        val existing = emergencyEventDao.getEmergencyEventById(id) ?: return
        emergencyEventDao.insertEmergencyEvent(
            existing.copy(status = DbEmergencyStatus.ACKNOWLEDGED)
        )
    }
}
