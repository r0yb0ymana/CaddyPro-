package com.caddypro.app.di

import android.content.Context
import androidx.room.Room
import com.caddypro.app.data.local.CaddyProDatabase
import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.dao.PlayerProfileDao
import com.caddypro.app.data.local.dao.CourseOverlayDao
import com.caddypro.app.data.local.dao.RoundDao
import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCaddyProDatabase(
        @ApplicationContext context: Context
    ): CaddyProDatabase {
        return Room.databaseBuilder(
            context,
            CaddyProDatabase::class.java,
            CaddyProDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePlayerProfileDao(database: CaddyProDatabase): PlayerProfileDao {
        return database.playerProfileDao()
    }

    @Provides
    @Singleton
    fun provideBagDao(database: CaddyProDatabase): BagDao {
        return database.bagDao()
    }

    @Provides
    @Singleton
    fun provideClubDao(database: CaddyProDatabase): ClubDao {
        return database.clubDao()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(database: CaddyProDatabase): WeatherDao {
        return database.weatherDao()
    }

    @Provides
    @Singleton
    fun provideRoundDao(database: CaddyProDatabase): RoundDao {
        return database.roundDao()
    }

    @Provides
    @Singleton
    fun provideShotDao(database: CaddyProDatabase): ShotDao {
        return database.shotDao()
    }

    @Provides
    @Singleton
    fun provideCourseOverlayDao(database: CaddyProDatabase): CourseOverlayDao {
        return database.courseOverlayDao()
    }
}
