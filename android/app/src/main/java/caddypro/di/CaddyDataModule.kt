package caddypro.di

import caddypro.BuildConfig
import caddypro.data.caddy.remote.WeatherApiService
import caddypro.data.caddy.repository.ClubBagRepositoryImpl
import caddypro.data.caddy.repository.MissPatternRepositoryImpl
import caddypro.data.caddy.repository.ReadinessRepository
import caddypro.data.caddy.repository.ReadinessRepositoryImpl
import caddypro.data.caddy.repository.SyncQueueRepository
import caddypro.data.caddy.repository.SyncQueueRepositoryImpl
import caddypro.data.caddy.repository.WeatherRepository
import caddypro.data.caddy.repository.WeatherRepositoryImpl
import caddypro.domain.caddy.repositories.ClubBagRepository
import caddypro.domain.caddy.repositories.MissPatternRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing Live Caddy feature data layer dependencies.
 *
 * Provides weather API service, repository, and API key configuration.
 * Also provides placeholder repository implementations for miss patterns and club bags
 * that delegate to NavCaddyRepository for MVP.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD), R3 (BodyCaddy), R4 (PinSeeker AI Map), R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 5, Task 8, Task 10, Task 19
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds), A2 (Readiness impacts strategy), A3 (Hazard-aware landing zone), A4 (Shot logger persistence)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaddyDataModule {

    /**
     * Bind WeatherRepository interface to implementation.
     */
    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    /**
     * Bind ReadinessRepository interface to implementation.
     *
     * Provides offline-first persistence for readiness scores with DAO access.
     */
    @Binds
    @Singleton
    abstract fun bindReadinessRepository(impl: ReadinessRepositoryImpl): ReadinessRepository

    /**
     * Bind SyncQueueRepository interface to implementation.
     *
     * Provides offline-first sync queue for shot logging with background sync.
     */
    @Binds
    @Singleton
    abstract fun bindSyncQueueRepository(impl: SyncQueueRepositoryImpl): SyncQueueRepository

    /**
     * Bind MissPatternRepository interface to placeholder implementation.
     *
     * For MVP, delegates to NavCaddyRepository.
     * Future: Extract to dedicated miss pattern storage.
     */
    @Binds
    @Singleton
    abstract fun bindMissPatternRepository(impl: MissPatternRepositoryImpl): MissPatternRepository

    /**
     * Bind ClubBagRepository interface to placeholder implementation.
     *
     * For MVP, delegates to NavCaddyRepository.
     * Future: Extract to dedicated bag profile storage.
     */
    @Binds
    @Singleton
    abstract fun bindClubBagRepository(impl: ClubBagRepositoryImpl): ClubBagRepository

    companion object {

        /**
         * Provide WeatherApiService configured for OpenWeatherMap API.
         *
         * Uses shared OkHttpClient from AppModule for consistent timeout
         * and logging configuration.
         *
         * Base URL: https://api.openweathermap.org/data/2.5/
         */
        @Provides
        @Singleton
        fun provideWeatherApiService(okHttpClient: OkHttpClient): WeatherApiService {
            return Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
        }

        /**
         * Provide Weather API key from BuildConfig.
         *
         * The API key is loaded from local.properties during build.
         * See app/build.gradle.kts for configuration.
         *
         * @return OpenWeatherMap API key
         */
        @Provides
        @Named("WeatherApiKey")
        fun provideWeatherApiKey(): String = BuildConfig.WEATHER_API_KEY
    }
}
