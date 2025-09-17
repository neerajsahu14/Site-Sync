package com.a1.sitesync.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a1.sitesync.data.models.Gate
import com.a1.sitesync.data.repository.LocalGateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the OverlayScreen.
 * Manages the state for gate selection and overlay manipulation.
 */
class OverlayViewModel(private val localGateRepository: LocalGateRepository) : ViewModel() {

    private val _gates = MutableStateFlow<List<Gate>>(emptyList())
    val gates = _gates.asStateFlow()

    private val _selectedGate = MutableStateFlow<Gate?>(null)
    val selectedGate = _selectedGate.asStateFlow()

    // --- Overlay State --- //
    private val _overlayOffset = MutableStateFlow(Offset.Zero)
    val overlayOffset = _overlayOffset.asStateFlow()

    private val _overlayScale = MutableStateFlow(1f)
    val overlayScale = _overlayScale.asStateFlow()

    private val _overlayRotation = MutableStateFlow(0f)
    val overlayRotation = _overlayRotation.asStateFlow()

    init {
        fetchGates()
    }

    private fun fetchGates() {
        viewModelScope.launch {
            _gates.value = localGateRepository.getLocalGates()
        }
    }

    fun onGateSelected(gate: Gate) {
        _selectedGate.value = gate
    }

    fun onOverlayDrag(delta: Offset) {
        _overlayOffset.value += delta
    }

    fun onOverlayZoom(zoom: Float) {
        _overlayScale.value *= zoom
    }

    fun onOverlayRotate(rotation: Float) {
        _overlayRotation.value += rotation
    }

    /**
     * Resets the overlay's position, scale, and rotation to their default values.
     */
    fun resetTransformations() {
        _overlayOffset.value = Offset.Zero
        _overlayScale.value = 1f
        _overlayRotation.value = 0f
    }
}
