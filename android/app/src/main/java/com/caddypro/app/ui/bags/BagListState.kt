package com.caddypro.app.ui.bags

import com.caddypro.app.domain.model.Bag

/**
 * UI State for BagListScreen
 */
data class BagListState(
    val bags: List<Bag> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val bagToDelete: Bag? = null,
    val profileId: String = ""
)
