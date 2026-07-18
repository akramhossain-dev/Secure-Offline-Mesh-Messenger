/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import android.content.Context
import androidx.room.Room
import com.mesh.emergency.data.local.database.AppDatabase

/**
 * Utility class providing in-memory database setups for Instrumented/UI Tests.
 */
object TestDatabaseHelper {

    /**
     * Creates an in-memory [AppDatabase] instance.
     * Allow main thread queries to simplify test operations.
     */
    fun createInMemoryDatabase(context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
    }
}
