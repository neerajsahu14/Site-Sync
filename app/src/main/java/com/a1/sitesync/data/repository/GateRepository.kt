package com.a1.sitesync.data.repository

import com.a1.sitesync.data.models.Gate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching gate model data from Firestore.
 */
class GateRepository(private val firestore: FirebaseFirestore) {

    /**
     * Fetches all gate models from the "gates" collection.
     *
     * @return A list of Gate objects.
     */
    suspend fun getGates(): List<Gate> {
        return try {
            val snapshot = firestore.collection("gates").get().await()
            snapshot.toObjects(Gate::class.java)
        } catch (e: Exception) {
            // In a real app, you'd want to log this error.
            e.printStackTrace()
            emptyList()
        }
    }
}
