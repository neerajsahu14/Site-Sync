package com.a1.sitesync.data.repository

import com.a1.sitesync.R
import com.a1.sitesync.data.models.Gate

/**
 * A repository that provides a static list of gate designs from local drawable resources.
 */
class LocalGateRepository {

    /**
     * Returns a hardcoded list of gate designs.
     * 
     * IMPORTANT: You must add your own gate design images (e.g., gate_design_1.png)
     * to the app/src/main/res/drawable/ folder. Then, uncomment the lines below
     * and replace the placeholders with your actual resource IDs.
     */
    fun getLocalGates(): List<Gate> {
        return listOf(
             Gate(name = "Classic Spear", imageResourceId = R.drawable.doorone),
             Gate(name = "Modern Slat", imageResourceId = R.drawable.doortwo),
            Gate(name = "Wooden Gate", imageResourceId = R.drawable.doorthree),
            Gate(name = "Open Door", imageResourceId = R.drawable.opendoorone)
        )
    }
}
