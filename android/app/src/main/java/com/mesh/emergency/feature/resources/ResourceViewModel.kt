/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.resource.ResourceManager
import com.mesh.emergency.data.local.entity.DbResourcePrivacy
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.domain.repository.ResourceDomainModel
import com.mesh.emergency.domain.repository.ResourceRepository
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Resource Sharing screen.
 *
 * Integrates with [ResourceManager] (A17) and [ResourceRepository]
 * to provide full CRUD for emergency resource sharing.
 */
@HiltViewModel
class ResourceViewModel @Inject constructor(
    private val resourceManager: ResourceManager,
    private val resourceRepository: ResourceRepository,
    private val deviceFingerprintProvider: DeviceFingerprintProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResourceUiState())
    val uiState: StateFlow<ResourceUiState> = _uiState.asStateFlow()

    init {
        loadResources()
        triggerExpiry()
    }

    private fun loadResources() {
        viewModelScope.launch {
            resourceRepository.getResources().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            allResources = result.data,
                            filteredResources = applyFilter(result.data, state.selectedCategory, state.selectedTab)
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.exception.message)
                    }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun triggerExpiry() {
        viewModelScope.launch {
            resourceManager.expireResources()
        }
    }

    fun onTabSelected(tab: ResourceTab) {
        _uiState.update { state ->
            state.copy(
                selectedTab = tab,
                filteredResources = applyFilter(state.allResources, state.selectedCategory, tab)
            )
        }
    }

    fun onCategorySelected(category: ResourceCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredResources = applyFilter(state.allResources, category, state.selectedTab)
            )
        }
    }

    fun onCreateOfferClicked() {
        _uiState.update { it.copy(showCreateDialog = true, dialogMode = ResourceDialogMode.OFFER) }
    }

    fun onCreateRequestClicked() {
        _uiState.update { it.copy(showCreateDialog = true, dialogMode = ResourceDialogMode.REQUEST) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showCreateDialog = false, selectedResource = null) }
    }

    fun onResourceSelected(resource: ResourceDomainModel) {
        _uiState.update { it.copy(selectedResource = resource) }
    }

    fun onCreateOffer(
        name: String,
        category: ResourceCategory,
        quantity: Int,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(showCreateDialog = false) }
            val result = resourceManager.createOffer(
                name = name,
                type = category.key,
                quantity = quantity,
                latitude = 0.0,
                longitude = 0.0,
                description = description,
                privacy = DbResourcePrivacy.PUBLIC.name
            )
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.exception.message) }
            }
        }
    }

    fun onCreateRequest(category: ResourceCategory, quantity: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(showCreateDialog = false) }
            val result = resourceManager.createRequest(
                type = category.key,
                requiredQuantity = quantity,
                priority = "HIGH"
            )
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.exception.message) }
            }
        }
    }

    fun onMarkUnavailable(resourceId: String) {
        viewModelScope.launch {
            resourceManager.updateAvailability(resourceId, DbResourceStatus.UNAVAILABLE.name)
            _uiState.update { it.copy(selectedResource = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilter(
        all: List<ResourceDomainModel>,
        category: ResourceCategory?,
        tab: ResourceTab
    ): List<ResourceDomainModel> {
        val tabFiltered = when (tab) {
            ResourceTab.AVAILABLE -> all.filter { it.status == DbResourceStatus.AVAILABLE.name || it.status == DbResourceStatus.LIMITED.name }
            ResourceTab.MY_RESOURCES -> all.filter { it.ownerId == deviceFingerprintProvider.getDeviceFingerprint() }
            ResourceTab.REQUESTS -> all.filter { it.status == DbResourceStatus.LIMITED.name }
        }
        return if (category == null) tabFiltered
        else tabFiltered.filter { it.type.equals(category.key, ignoreCase = true) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI state models
// ─────────────────────────────────────────────────────────────────────────────

data class ResourceUiState(
    val isLoading: Boolean = true,
    val allResources: List<ResourceDomainModel> = emptyList(),
    val filteredResources: List<ResourceDomainModel> = emptyList(),
    val selectedTab: ResourceTab = ResourceTab.AVAILABLE,
    val selectedCategory: ResourceCategory? = null,
    val showCreateDialog: Boolean = false,
    val dialogMode: ResourceDialogMode = ResourceDialogMode.OFFER,
    val selectedResource: ResourceDomainModel? = null,
    val errorMessage: String? = null
)

enum class ResourceTab(val label: String) {
    AVAILABLE("Available"),
    MY_RESOURCES("Mine"),
    REQUESTS("Requests")
}

enum class ResourceDialogMode { OFFER, REQUEST }

/**
 * Emergency resource categories per A32.3 spec.
 */
enum class ResourceCategory(val label: String, val key: String, val emoji: String) {
    MEDICAL("Medical",   "MEDICAL",   "🏥"),
    FOOD("Food",         "FOOD",      "🍞"),
    SHELTER("Shelter",   "SHELTER",   "🏠"),
    EQUIPMENT("Equipment","EQUIPMENT","🔧"),
    SERVICE("Service",   "SERVICE",   "🤝")
}
