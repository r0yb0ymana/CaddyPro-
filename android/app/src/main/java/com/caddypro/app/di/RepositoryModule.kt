package com.caddypro.app.di

import com.caddypro.app.data.repository.BagRepositoryImpl
import com.caddypro.app.data.repository.ClubRepositoryImpl
import com.caddypro.app.data.repository.ProfileRepositoryImpl
import com.caddypro.app.data.repository.CourseDataRepositoryImpl
import com.caddypro.app.data.repository.RoundRepositoryImpl
import com.caddypro.app.data.repository.ShotRepositoryImpl
import com.caddypro.app.data.repository.WeatherRepositoryImpl
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ClubRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.repository.CourseDataRepository
import com.caddypro.app.domain.repository.RoundRepository
import com.caddypro.app.domain.repository.ShotRepository
import com.caddypro.app.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindBagRepository(
        bagRepositoryImpl: BagRepositoryImpl
    ): BagRepository

    @Binds
    @Singleton
    abstract fun bindClubRepository(
        clubRepositoryImpl: ClubRepositoryImpl
    ): ClubRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindRoundRepository(
        roundRepositoryImpl: RoundRepositoryImpl
    ): RoundRepository

    @Binds
    @Singleton
    abstract fun bindShotRepository(
        shotRepositoryImpl: ShotRepositoryImpl
    ): ShotRepository

    @Binds
    @Singleton
    abstract fun bindCourseDataRepository(
        courseDataRepositoryImpl: CourseDataRepositoryImpl
    ): CourseDataRepository
}
