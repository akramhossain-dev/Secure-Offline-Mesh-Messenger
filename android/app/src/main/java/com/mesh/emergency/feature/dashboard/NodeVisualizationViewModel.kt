/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Node Visualization screen.
 *
 * Exposes known mesh nodes with filtering by status.
 * No real mesh routing is performed.
 */
@HiltViewModel
class NodeVisualizationViewModel @Inject constructor(
    private val nodeRepository: NodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeVisualizationUiState())
    val uiState: StateFlow<NodeVisualizationUiState> = _uiState.asStateFlow()

    init {
        observeNodes()
    }

    private fun observeNodes() {
        viewModelScope.launch {
            nodeRepository.getNetworkNodes().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val nodes = result.data
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                allNodes = nodes,
                                filteredNodes = applyFilter(nodes, state.selectedFilter)
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.exception.message)
                    }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onFilterSelected(filter: NodeFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredNodes = applyFilter(state.allNodes, filter)
            )
        }
    }

    fun onNodeSelected(node: NodeDomainModel) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun onNodeDismissed() {
        _uiState.update { it.copy(selectedNode = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilter(nodes: List<NodeDomainModel>, filter: NodeFilter): List<NodeDomainModel> {
        return when (filter) {
            NodeFilter.ALL     -> nodes
            NodeFilter.ONLINE  -> nodes.filter { it.status == "ONLINE" }
            NodeFilter.OFFLINE -> nodes.filter { it.status == "OFFLINE" || it.status == "UNKNOWN" }
            NodeFilter.RELAY   -> nodes.filter { it.relayCapability }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI state
// ─────────────────────────────────────────────────────────────────────────────

data class NodeVisualizationUiState(
    val isLoading: Boolean = true,
    val allNodes: List<NodeDomainModel> = emptyList(),
    val filteredNodes: List<NodeDomainModel> = emptyList(),
    val selectedFilter: NodeFilter = NodeFilter.ALL,
    val selectedNode: NodeDomainModel? = null,
    val errorMessage: String? = null
)

enum class NodeFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    OFFLINE("Offline"),
    RELAY("Relay")
}
