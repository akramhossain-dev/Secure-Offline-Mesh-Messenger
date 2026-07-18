/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local

import timber.log.Timber

/**
 * Database optimization utilities for Room.
 *
 * Provides a default page size constant and helper functions for
 * pagination calculations used across DAOs.
 *
 * WAL mode and cache size pragmas are applied in the DatabaseModule
 * via RoomDatabase.Builder callback.
 */
object DatabaseOptimizer {

    /** Default page size for paginated queries. */
    const val DEFAULT_PAGE_SIZE = 20

    /** Large page size for bulk operations. */
    const val LARGE_PAGE_SIZE = 50

    /** Returns the OFFSET value for a given page index. */
    fun offsetFor(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE): Int = page * pageSize

    /**
     * Calculates total page count for a given item count.
     */
    fun totalPages(totalCount: Int, pageSize: Int = DEFAULT_PAGE_SIZE): Int =
        if (totalCount == 0) 0 else (totalCount + pageSize - 1) / pageSize

    /**
     * Logs a query timing measurement for performance monitoring.
     */
    fun logQueryTime(tag: String, startMs: Long) {
        val elapsed = System.currentTimeMillis() - startMs
        if (elapsed > SLOW_QUERY_THRESHOLD_MS) {
            Timber.w("DB_PERF: SLOW QUERY [$tag] took ${elapsed}ms")
        } else {
            Timber.d("DB_PERF: [$tag] ${elapsed}ms")
        }
    }

    /**
     * SQLite WAL pragma SQL for faster concurrent reads.
     * Apply via SupportSQLiteDatabase.execSQL() in Room open callback.
     */
    const val PRAGMA_WAL_MODE = "PRAGMA journal_mode=WAL"

    /**
     * SQLite cache size pragma (4 MB).
     * Apply via SupportSQLiteDatabase.execSQL() in Room open callback.
     */
    const val PRAGMA_CACHE_SIZE = "PRAGMA cache_size=4096"

    /** Queries taking longer than this are logged as warnings. */
    private const val SLOW_QUERY_THRESHOLD_MS = 100
}
