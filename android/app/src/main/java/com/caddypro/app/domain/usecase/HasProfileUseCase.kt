package com.caddypro.app.domain.usecase

import com.caddypro.app.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Use case to check if a user profile exists
 *
 * Used for first-launch detection:
 * - If no profile exists, route to ProfileSetupScreen
 * - If profile exists, route to BagList (main app)
 */
class HasProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): Boolean {
        return profileRepository.hasProfile()
    }
}
