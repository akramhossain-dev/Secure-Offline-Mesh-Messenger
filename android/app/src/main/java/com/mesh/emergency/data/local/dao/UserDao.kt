/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesh.emergency.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'users' table.
 */
@Dao
interface UserDao {

    /** Returns current user profile details stream. */
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    /** Stream of all stored contacts. */
    @Query("SELECT * FROM users WHERE isCurrentUser = 0 ORDER BY lastSeen DESC")
    fun getContacts(): Flow<List<UserEntity>>

    /** Queries user records matching details ID. */
    @Query("SELECT * FROM users WHERE entityId = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    /** Insert or replace user record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /** Delete user record. */
    @Delete
    suspend fun deleteUser(user: UserEntity)
}
