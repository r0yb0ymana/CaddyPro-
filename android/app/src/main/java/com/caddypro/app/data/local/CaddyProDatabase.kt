package com.caddypro.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.dao.PlayerProfileDao
import com.caddypro.app.data.local.dao.RoundDao
import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.dao.CourseOverlayDao
import com.caddypro.app.data.local.dao.WeatherDao
import com.caddypro.app.data.local.entities.BagEntity
import com.caddypro.app.data.local.entities.CourseOverlayEntity
import com.caddypro.app.data.local.entities.ClubEntity
import com.caddypro.app.data.local.entities.PlayerProfileEntity
import com.caddypro.app.data.local.entities.RoundEntity
import com.caddypro.app.data.local.entities.ShotEntity
import com.caddypro.app.data.local.entities.WeatherEntity

/**
 * CaddyPro Room Database
 *
 * Offline-first local database for all CaddyPro data.
 */
@Database(
    entities = [
        PlayerProfileEntity::class,
        BagEntity::class,
        ClubEntity::class,
        WeatherEntity::class,
        RoundEntity::class,
        ShotEntity::class,
        CourseOverlayEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CaddyProDatabase : RoomDatabase() {

    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun bagDao(): BagDao
    abstract fun clubDao(): ClubDao
    abstract fun weatherDao(): WeatherDao
    abstract fun roundDao(): RoundDao
    abstract fun shotDao(): ShotDao
    abstract fun courseOverlayDao(): CourseOverlayDao

    companion object {
        const val DATABASE_NAME = "caddypro.db"
    }
}
