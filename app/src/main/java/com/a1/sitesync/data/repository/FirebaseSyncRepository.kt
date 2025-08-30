package com.a1.sitesync.data.repository

import com.a1.sitesync.data.models.FirestoreSurvey
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository {
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val collection = firestoreDb.collection("surveys")

    // Create or Update (set)
    suspend fun upsertSurvey(survey: FirestoreSurvey) {
        collection.document(survey.surveyId).set(survey).await()
    }

    // Read all
    suspend fun getAllSurveys(): List<FirestoreSurvey> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.toObject(FirestoreSurvey::class.java) }
    }

    // Read by ID
    suspend fun getSurveyById(surveyId: String): FirestoreSurvey? {
        val doc = collection.document(surveyId).get().await()
        return doc.toObject(FirestoreSurvey::class.java)
    }

    // Delete
    suspend fun deleteSurvey(surveyId: String) {
        collection.document(surveyId).delete().await()
    }
}