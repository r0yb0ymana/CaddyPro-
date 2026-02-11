package com.caddypro.app.ui.bags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.domain.model.Bag
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for BagListScreen
 *
 * Manages bag CRUD operations with business rules:
 * - AC6: Default "My Bag" created on first launch
 * - AC7: Only one bag active at a time
 * - AC8: Deleting active bag promotes next bag
 * - AC9: Cannot delete last bag
 * - AC10: Real-time updates via Flow
 */
@HiltViewModel
class BagListViewModel @Inject constructor(
    private val bagRepository: BagRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BagListState())
    val uiState: StateFlow<BagListState> = _uiState.asStateFlow()

    init {
        loadBags()
    }

    /**
     * Load bags for the current profile
     * AC10: Real-time updates via Flow
     */
    private fun loadBags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get profile ID from the current profile
                val profile = profileRepository.getProfile().firstOrNull()
                val profileId = profile?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No profile found"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(profileId = profileId) }

                // Observe bags for this profile
                bagRepository.getBagsByProfileId(profileId).collect { bags ->
                    _uiState.update {
                        it.copy(
                            bags = bags,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load bags: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Handle user actions
     */
    fun onAction(action: BagListAction) {
        when (action) {
            is BagListAction.SetActiveBag -> setActiveBag(action.bag)
            is BagListAction.ShowDeleteConfirmation -> showDeleteConfirmation(action.bag)
            is BagListAction.DismissDeleteConfirmation -> dismissDeleteConfirmation()
            is BagListAction.ConfirmDelete -> confirmDelete()
            is BagListAction.CreateNewBag -> createNewBag()
            is BagListAction.ClearError -> clearError()
            is BagListAction.NavigateToClubEditor -> {
                // Navigation handled in UI
            }
        }
    }

    /**
     * AC7: Set a bag as active (deactivates all others)
     */
    private fun setActiveBag(bag: Bag) {
        if (bag.isActive) return // Already active

        viewModelScope.launch {
            try {
                bagRepository.setActiveBag(_uiState.value.profileId, bag.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to set active bag: ${e.message}")
                }
            }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(bag: Bag) {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = true,
                bagToDelete = bag
            )
        }
    }

    /**
     * Dismiss delete confirmation dialog
     */
    private fun dismissDeleteConfirmation() {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = false,
                bagToDelete = null
            )
        }
    }

    /**
     * Confirm bag deletion
     * AC8: Deleting active bag promotes next bag
     * AC9: Cannot delete last bag
     */
    private fun confirmDelete() {
        val bagToDelete = _uiState.value.bagToDelete ?: return

        viewModelScope.launch {
            try {
                val result = bagRepository.deleteBag(
                    _uiState.value.profileId,
                    bagToDelete.id
                )

                result.fold(
                    onSuccess = {
                        // Success - dismiss dialog
                        _uiState.update {
                            it.copy(
                                showDeleteConfirmation = false,
                                bagToDelete = null
                            )
                        }
                    },
                    onFailure = { error ->
                        // AC9: Show error if trying to delete last bag
                        _uiState.update {
                            it.copy(
                                showDeleteConfirmation = false,
                                bagToDelete = null,
                                errorMessage = error.message ?: "Failed to delete bag"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        bagToDelete = null,
                        errorMessage = "Failed to delete bag: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Create a new bag
     */
    private fun createNewBag() {
        viewModelScope.launch {
            try {
                val newBag = Bag(
                    profileId = _uiState.value.profileId,
                    name = "New Bag ${_uiState.value.bags.size + 1}",
                    isActive = false
                )
                bagRepository.createBag(newBag)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to create bag: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
