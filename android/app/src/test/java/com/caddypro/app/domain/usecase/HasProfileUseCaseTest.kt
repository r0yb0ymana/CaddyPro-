package com.caddypro.app.domain.usecase

import com.caddypro.app.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HasProfileUseCase
 *
 * Tests first-launch detection logic
 */
class HasProfileUseCaseTest {

    private lateinit var hasProfileUseCase: HasProfileUseCase
    private lateinit var profileRepository: ProfileRepository

    @Before
    fun setup() {
        profileRepository = mockk()
        hasProfileUseCase = HasProfileUseCase(profileRepository)
    }

    @Test
    fun `returns true when profile exists`() = runTest {
        coEvery { profileRepository.hasProfile() } returns true

        val result = hasProfileUseCase()

        assertTrue(result)
        coVerify { profileRepository.hasProfile() }
    }

    @Test
    fun `returns false when no profile exists`() = runTest {
        coEvery { profileRepository.hasProfile() } returns false

        val result = hasProfileUseCase()

        assertFalse(result)
        coVerify { profileRepository.hasProfile() }
    }
}
