package caddypro.di

import caddypro.domain.navcaddy.clarification.ClarificationHandler
import caddypro.domain.navcaddy.classifier.IntentClassifier
import caddypro.domain.navcaddy.llm.GeminiClient
import caddypro.domain.navcaddy.llm.LLMClient
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator
import caddypro.domain.navcaddy.navigation.NoOpNavCaddyNavigator
import caddypro.domain.navcaddy.normalizer.InputNormalizer
import caddypro.domain.navcaddy.routing.PrerequisiteChecker
import caddypro.domain.navcaddy.routing.RoutingOrchestrator
import caddypro.domain.navcaddy.routing.Prerequisite
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing NavCaddy domain layer dependencies.
 *
 * Provides intent classification, routing, and navigation services.
 *
 * Spec reference: navcaddy-engine.md R2, R3
 * Plan reference: navcaddy-engine-plan.md Tasks 6-13, 18
 */
@Module
@InstallIn(SingletonComponent::class)
object NavCaddyDomainModule {

    /**
     * Provide LLM client for intent classification.
     *
     * Currently provides GeminiClient. Can be swapped for testing or production.
     */
    @Provides
    @Singleton
    fun provideLLMClient(): LLMClient {
        return GeminiClient(
            apiKey = "" // TODO: Load from BuildConfig or secure storage in production
        )
    }

    /**
     * Provide input normalizer.
     */
    @Provides
    @Singleton
    fun provideInputNormalizer(): InputNormalizer {
        return InputNormalizer()
    }

    /**
     * Provide clarification handler.
     */
    @Provides
    @Singleton
    fun provideClarificationHandler(): ClarificationHandler {
        return ClarificationHandler()
    }

    /**
     * Provide intent classifier.
     */
    @Provides
    @Singleton
    fun provideIntentClassifier(
        llmClient: LLMClient,
        normalizer: InputNormalizer,
        clarificationHandler: ClarificationHandler
    ): IntentClassifier {
        return IntentClassifier(
            llmClient = llmClient,
            normalizer = normalizer,
            clarificationHandler = clarificationHandler
        )
    }

    /**
     * Provide prerequisite checker.
     */
    @Provides
    @Singleton
    fun providePrerequisiteChecker(): PrerequisiteChecker {
        // TODO: Implement real PrerequisiteChecker with data sources
        return object : PrerequisiteChecker {
            override suspend fun check(prerequisite: Prerequisite): Boolean {
                // For now, always return true (prerequisite satisfied)
                // In production, this would check actual data sources
                return true
            }
        }
    }

    /**
     * Provide routing orchestrator.
     */
    @Provides
    @Singleton
    fun provideRoutingOrchestrator(
        prerequisiteChecker: PrerequisiteChecker
    ): RoutingOrchestrator {
        return RoutingOrchestrator(prerequisiteChecker)
    }

    /**
     * Provide NavCaddy navigator.
     *
     * For now using NoOp implementation. In production, this would be
     * provided by the UI layer with actual NavController integration.
     */
    @Provides
    @Singleton
    fun provideNavCaddyNavigator(): NavCaddyNavigator {
        // TODO: Replace with actual navigator implementation in UI layer
        // The UI layer should provide this via a separate module or directly in MainActivity
        return NoOpNavCaddyNavigator()
    }
}
