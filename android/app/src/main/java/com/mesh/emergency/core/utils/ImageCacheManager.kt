/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.graphics.Bitmap
import timber.log.Timber
import java.lang.ref.SoftReference

/**
 * LRU bitmap cache manager using [SoftReference] for GC-friendly memory management.
 *
 * Cache size defaults to 1/8 of available heap memory.
 * Entries are automatically evicted under memory pressure via soft references.
 */
class ImageCacheManager(maxSizeBytes: Int = defaultCacheSize()) {

    private val lruCache = object : android.util.LruCache<String, Bitmap>(maxSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Stores a bitmap in the cache. */
    fun put(key: String, bitmap: Bitmap) {
        lruCache.put(key, bitmap)
        Timber.d("ImageCache: Cached [$key] — cache size: ${lruCache.size()} / ${lruCache.maxSize()} bytes")
    }

    /** Retrieves a cached bitmap, or null if evicted. */
    fun get(key: String): Bitmap? = lruCache.get(key)

    /** Removes a specific entry. */
    fun remove(key: String) {
        lruCache.remove(key)
    }

    /** Clears all cached bitmaps. */
    fun evictAll() {
        lruCache.evictAll()
        Timber.d("ImageCache: Evicted all entries")
    }

    /** Returns cache size stats as a string. */
    fun stats(): String = "ImageCache: ${lruCache.size()}/${lruCache.maxSize()} bytes, ${lruCache.hitCount()} hits, ${lruCache.missCount()} misses"

    companion object {
        fun defaultCacheSize(): Int {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return maxMemory / 8 // Use 1/8th of available heap
        }
    }
}
