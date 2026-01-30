package caddypro.di

import caddypro.data.caddy.wearable.StubWearableSyncService
import caddypro.domain.caddy.services.WearableSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Live Caddy feature domain layer dependencies.
 *
 * Provides wearable sync service and readiness calculation dependencies.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 * Acceptance criteria: A2 (Readiness impacts strategy)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaddyDomainModule {

    /**
     * Bind WearableSyncService interface to stub implementation.
     *
     * MVP uses StubWearableSyncService for testing without real wearable integration.
     *
     * TODO: Replace with real implementations:
     * - Android: GoogleFitSyncService (Health Connect API)
     * - iOS: AppleHealthKitSyncService
     * - Third-party: GarminSyncService, WhoopSyncService, OuraSyncService
     *
     * Consider using qualifier annotations to support multiple implementations:
     * @Binds @Named("GoogleFit") fun bindGoogleFit(...)
     * @Binds @Named("AppleHealth") fun bindAppleHealth(...)
     */
    @Binds
    @Singleton
    abstract fun bindWearableSyncService(impl: StubWearableSyncService): WearableSyncService
}
