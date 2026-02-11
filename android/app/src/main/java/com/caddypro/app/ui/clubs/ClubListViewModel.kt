package com.caddypro.app.ui.clubs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ClubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ClubListScreen
 *
 * AC11: Quick Add populates standard 14-club set
 * AC13: Club type determines sort order
 * AC15: Maximum 14 clubs per bag with warning
 */
@HiltViewModel
class ClubListViewModel @Inject constructor(
    private val clubRepository: ClubRepository,
    private val bagRepository: BagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bagId: String = checkNotNull(savedStateHandle["bagId"])

    private val _uiState = MutableStateFlow(ClubListState())
    val uiState: StateFlow<ClubListState> = _uiState.asStateFlow()

    init {
        loadBag()
        loadClubs()
    }

    /**
     * Load bag details
     */
    private fun loadBag() {
        viewModelScope.launch {
            try {
                val bag = bagRepository.getBagById(bagId)
                if (bag != null) {
                    _uiState.update {
                        it.copy(
                            bagId = bagId,
                            bagName = bag.name
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to load bag: ${e.message}")
                }
            }
        }
    }

    /**
     * Load clubs for this bag
     * AC13: Clubs are sorted by type (Driver first, Putter last)
     */
    private fun loadClubs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                clubRepository.getClubsByBagId(bagId).collect { clubs ->
                    // Group clubs by type for UI display
                    val clubsByType = clubs.groupBy { it.type }
                        .toSortedMap(compareBy { it.sortOrder })

                    val clubCount = clubs.size

                    _uiState.update {
                        it.copy(
                            clubs = clubs,
                            clubsByType = clubsByType,
                            clubCount = clubCount,
                            isLoading = false,
                            showMaxClubsWarning = clubCount == 14  // AC15: Warning at 14 clubs
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load clubs: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Handle user actions
     */
    fun onAction(action: ClubListAction) {
        when (action) {
            is ClubListAction.ShowAddClub -> showAddClub()
            is ClubListAction.EditClub -> editClub(action.club)
            is ClubListAction.DismissClubDetail -> dismissClubDetail()
            is ClubListAction.DeleteClub -> deleteClub(action.clubId)
            is ClubListAction.QuickAddStandardSet -> quickAddStandardSet()
            is ClubListAction.ClearError -> clearError()
            is ClubListAction.DismissMaxClubsWarning -> dismissMaxClubsWarning()
        }
    }

    /**
     * Show add club sheet
     * AC15: Block if already at 14 clubs
     */
    private fun showAddClub() {
        if (_uiState.value.isAtMaxClubs()) {
            _uiState.update {
                it.copy(errorMessage = "Maximum 14 clubs allowed per bag (tournament rules)")
            }
            return
        }

        _uiState.update {
            it.copy(
                showClubDetail = true,
                clubToEdit = null
            )
        }
    }

    private fun editClub(club: com.caddypro.app.domain.model.Club) {
        _uiState.update {
            it.copy(
                showClubDetail = true,
                clubToEdit = club
            )
        }
    }

    private fun dismissClubDetail() {
        _uiState.update {
            it.copy(
                showClubDetail = false,
                clubToEdit = null
            )
        }
    }

    private fun deleteClub(clubId: String) {
        viewModelScope.launch {
            try {
                val result = clubRepository.deleteClub(clubId)
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to delete club")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete club: ${e.message}")
                }
            }
        }
    }

    /**
     * AC11: Quick Add populates standard 14-club set with typical distances
     */
    private fun quickAddStandardSet() {
        viewModelScope.launch {
            try {
                val result = clubRepository.quickAddStandardSet(bagId)
                result.fold(
                    onSuccess = {
                        // Success - clubs are automatically loaded via Flow
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "Failed to add clubs")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to add clubs: ${e.message}")
                }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun dismissMaxClubsWarning() {
        _uiState.update { it.copy(showMaxClubsWarning = false) }
    }
}
