package caddypro.di

import caddypro.domain.navcaddy.memory.PatternDecayCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing NavCaddy memory layer dependencies.
 *
 * Provides decay calculator and memory management components.
 *
 * Spec reference: navcaddy-engine.md R5, Q5 (14-day decay half-life)
 */
@Module
@InstallIn(SingletonComponent::class)
object NavCaddyMemoryModule {

    /**
     * Provide PatternDecayCalculator with default 14-day half-life.
     *
     * Half-life can be configured for testing or different decay strategies.
     */
    @Provides
    @Singleton
    fun providePatternDecayCalculator(): PatternDecayCalculator {
        return PatternDecayCalculator(
            decayHalfLifeDays = PatternDecayCalculator.DEFAULT_HALF_LIFE_DAYS
        )
    }

    // Note: ShotRecorder, MissPatternAggregator, and MissPatternStore
    // are automatically provided by Hilt via @Inject constructor
}
