package com.a1.sitesync.data.models

import androidx.annotation.DrawableRes

/**
 * Represents a single, locally-stored gate design.
 * The image for the gate is stored in the app's drawable or raw resources.
 */
data class Gate(
    val name: String,
    @DrawableRes val imageResourceId: Int
)
