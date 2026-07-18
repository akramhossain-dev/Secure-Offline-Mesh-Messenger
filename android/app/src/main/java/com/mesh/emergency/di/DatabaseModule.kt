/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.LocalDataSourceImpl
import com.mesh.emergency.data.local.dao.MessageDao
import com.mesh.emergency.data.local.dao.ConversationDao
import com.mesh.emergency.data.local.dao.EmergencyEventDao
import com.mesh.emergency.data.local.database.AppDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local persistent storage interfaces and Room Database provider hooks.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    /** Binds [LocalDataSourceImpl] implementation to the [LocalDataSource] contract. */
    @Binds
    @Singleton
    abstract fun bindLocalDataSource(impl: LocalDataSourceImpl): LocalDataSource

    companion object {

        // ── Database Migration Strategy Framework ─────────────────────────────
        // Demonstrates the template used for upgrading offline schemas.
        // Incremental upgrades use migrations to ensure no user data is lost.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Future SQL schema modifications go here
                // e.g. db.execSQL("ALTER TABLE messages ADD COLUMN signature TEXT")
            }
        }

        /**
         * Provides the singleton database instance.
         */
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            // A33.3 — Enable WAL for faster concurrent reads + larger page cache
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.query("PRAGMA journal_mode=WAL").close()
                    db.query("PRAGMA cache_size=4096").close()
                    db.query("PRAGMA synchronous=NORMAL").close()
                }
            })
            .build()
        }

        @Provides
        @Singleton
        fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

        @Provides
        @Singleton
        fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

        @Provides
        @Singleton
        fun provideEmergencyEventDao(db: AppDatabase): EmergencyEventDao = db.emergencyEventDao()
    }
}
