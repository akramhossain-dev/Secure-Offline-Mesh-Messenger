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
import com.mesh.emergency.data.local.dao.GlobalMessageDao
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
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `global_messages` (
                        `messageId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `deliveryStatus` TEXT NOT NULL DEFAULT 'SENT',
                        PRIMARY KEY(`messageId`)
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_global_messages_timestamp` ON `global_messages` (`timestamp`)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Upgrade messages table
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `messageId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `senderName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `edited` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `readStatus` TEXT NOT NULL DEFAULT 'UNREAD'")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `editHistory` TEXT NOT NULL DEFAULT '[]'")

                // Upgrade global_messages table
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `edited` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `readStatus` TEXT NOT NULL DEFAULT 'READ'")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `editHistory` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Upgrade messages table
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToMessageId` TEXT")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToSenderName` TEXT")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToContent` TEXT")

                // Upgrade global_messages table
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `replyToMessageId` TEXT")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `replyToSenderName` TEXT")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `replyToContent` TEXT")
            }
        }

        /**
         * Migration 4 → 5: Transport independence metadata.
         * Adds [transportType] column to both message tables.
         * Default value 'UNKNOWN' ensures backward compatibility with all existing rows.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `transportType` TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE `global_messages` ADD COLUMN `transportType` TEXT NOT NULL DEFAULT 'UNKNOWN'")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
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

        @Provides
        @Singleton
        fun provideGlobalMessageDao(db: AppDatabase): GlobalMessageDao = db.globalMessageDao()
    }
}
