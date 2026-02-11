package com.caddypro.app.data.local

import androidx.room.TypeConverter
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import com.caddypro.app.domain.model.ShotType

/**
 * Room Type Converters for custom data types
 */
class Converters {

    @TypeConverter
    fun fromPreferredUnits(value: PreferredUnits): String {
        return value.name
    }

    @TypeConverter
    fun toPreferredUnits(value: String): PreferredUnits {
        return PreferredUnits.valueOf(value)
    }

    @TypeConverter
    fun fromClubType(value: ClubType): String {
        return value.name
    }

    @TypeConverter
    fun toClubType(value: String): ClubType {
        return ClubType.valueOf(value)
    }

    @TypeConverter
    fun fromMissBias(value: MissBias): String {
        return value.name
    }

    @TypeConverter
    fun toMissBias(value: String): MissBias {
        return MissBias.valueOf(value)
    }

    @TypeConverter
    fun fromShotType(value: ShotType): String {
        return value.name
    }

    @TypeConverter
    fun toShotType(value: String): ShotType {
        return ShotType.valueOf(value)
    }
}
