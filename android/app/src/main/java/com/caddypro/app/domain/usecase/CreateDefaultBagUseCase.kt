package com.caddypro.app.domain.usecase

import com.caddypro.app.domain.model.Bag
import com.caddypro.app.domain.repository.BagRepository
import javax.inject.Inject

/**
 * Use case to create the default bag for a new profile
 *
 * AC6: Default "My Bag" created on first launch
 * Creates a bag named "My Bag" set as active
 */
class CreateDefaultBagUseCase @Inject constructor(
    private val bagRepository: BagRepository
) {
    suspend operator fun invoke(profileId: String): Bag {
        return bagRepository.createDefaultBag(profileId)
    }
}
