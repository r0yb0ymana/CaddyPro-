package caddypro.di

import android.content.Context
import androidx.room.Room
import caddypro.BuildConfig
import caddypro.data.caddy.local.dao.ReadinessScoreDao
import caddypro.data.caddy.local.dao.SyncQueueDao
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.dao.BagClubDao
import caddypro.data.navcaddy.dao.BagProfileDao
import caddypro.data.navcaddy.dao.DistanceAuditDao
import caddypro.data.navcaddy.dao.MissPatternDao
import caddypro.data.navcaddy.dao.SessionDao
import caddypro.data.navcaddy.dao.ShotDao
import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.data.navcaddy.repository.NavCaddyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing NavCaddy data layer dependencies.
 *
 * Provides database, DAOs, and repository implementation.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4
 *                 player-profile-bag-management.md R1, R2, R3
 *                 live-caddy-mode.md R3 (BodyCaddy), R6 (Real-Time Shot Logger)
 */
@Module
@InstallIn(SingletonComponent::class)
object NavCaddyDataModule {

    /**
     * Provide the NavCaddy Room database.
     *
     * Includes migrations:
     * - v2 to v3: readiness_scores table
     * - v3 to v4: sync_queue table
     *
     * TODO: Add SQLCipher encryption for C4 compliance.
     */
    @Provides
    @Singleton
    fun provideNavCaddyDatabase(
        @ApplicationContext context: Context
    ): NavCaddyDatabase {
        return Room.databaseBuilder(
            context,
            NavCaddyDatabase::class.java,
            NavCaddyDatabase.DATABASE_NAME
        )
            // TODO: Add encryption via SQLCipher for sensitive data (C4)
            // .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes("passphrase".toCharArray())))
            .addMigrations(
                NavCaddyDatabase.MIGRATION_2_3,
                NavCaddyDatabase.MIGRATION_3_4
            )
            .apply {
                // Only allow destructive migration in debug builds to prevent data loss
                if (BuildConfig.DEBUG) {
                    fallbackToDestructiveMigration()
                }
            }
            .build()
    }

    /**
     * Provide ShotDao from database.
     */
    @Provides
    @Singleton
    fun provideShotDao(database: NavCaddyDatabase): ShotDao {
        return database.shotDao()
    }

    /**
     * Provide MissPatternDao from database.
     */
    @Provides
    @Singleton
    fun provideMissPatternDao(database: NavCaddyDatabase): MissPatternDao {
        return database.missPatternDao()
    }

    /**
     * Provide SessionDao from database.
     */
    @Provides
    @Singleton
    fun provideSessionDao(database: NavCaddyDatabase): SessionDao {
        return database.sessionDao()
    }

    /**
     * Provide BagProfileDao from database.
     */
    @Provides
    @Singleton
    fun provideBagProfileDao(database: NavCaddyDatabase): BagProfileDao {
        return database.bagProfileDao()
    }

    /**
     * Provide BagClubDao from database.
     */
    @Provides
    @Singleton
    fun provideBagClubDao(database: NavCaddyDatabase): BagClubDao {
        return database.bagClubDao()
    }

    /**
     * Provide DistanceAuditDao from database.
     */
    @Provides
    @Singleton
    fun provideDistanceAuditDao(database: NavCaddyDatabase): DistanceAuditDao {
        return database.distanceAuditDao()
    }

    /**
     * Provide ReadinessScoreDao from database.
     */
    @Provides
    @Singleton
    fun provideReadinessScoreDao(database: NavCaddyDatabase): ReadinessScoreDao {
        return database.readinessScoreDao()
    }

    /**
     * Provide SyncQueueDao from database.
     */
    @Provides
    @Singleton
    fun provideSyncQueueDao(database: NavCaddyDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
}

/**
 * Module for binding repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavCaddyRepositoryModule {

    /**
     * Bind NavCaddyRepository interface to implementation.
     */
    @Binds
    @Singleton
    abstract fun bindNavCaddyRepository(
        impl: NavCaddyRepositoryImpl
    ): NavCaddyRepository
}
