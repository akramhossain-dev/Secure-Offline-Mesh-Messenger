/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapRepository
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.data.map.MapProviderImpl
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the offline map screen.
 *
 * Combines map layer state, node positions, and user location for rendering.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val mapProvider: MapProviderImpl,
    private val nodeRepository: NodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val mapState: StateFlow<MapState> = mapProvider.mapState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState.EMPTY)

    init {
        observeLayers()
        observeNodes()
        mapProvider.markMapReady()
    }

    private fun observeLayers() {
        viewModelScope.launch {
            mapRepository.getMapLayers().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(layers = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(errorMessage = result.exception.message) }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            nodeRepository.getNetworkNodes().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            visibleNodes = result.data.filter { n ->
                                n.latitude != 0.0 && n.longitude != 0.0
                            }
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onLayerToggled(layerId: String, isVisible: Boolean) {
        mapProvider.setLayerVisible(layerId, isVisible)
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    if (layer.id == layerId) layer.copy(isVisible = isVisible) else layer
                }
            )
        }
    }

    fun onZoomIn() {
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel + 1).coerceAtMost(18)) }
    }

    fun onZoomOut() {
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel - 1).coerceAtLeast(1)) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the offline map screen.
 */
data class MapUiState(
    val isLoading: Boolean = false,
    val layers: List<MapLayerModel> = emptyList(),
    val visibleNodes: List<NodeDomainModel> = emptyList(),
    val zoomLevel: Int = 10,
    val errorMessage: String? = null
)
