package com.a1.sitesync.data.service

import com.a1.sitesync.data.models.FirestoreSurvey
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * A dedicated service to handle all communications with Firebase services.
 * This isolates Firebase-specific code from the main repository, improving
 * modularity and making the code easier to test and maintain.
 */
class FirebaseSyncService(
    private val cloudinaryService: CloudinarySyncService
) {
    // Get instances of Firestore and Cloud Storage.
    private val firestoreDb = FirebaseFirestore.getInstance()

    /**
     * Uploads a file to Cloudinary via CloudinarySyncService
     */
    suspend fun uploadFile(localFile: File, surveyId: String): String {
        return cloudinaryService.uploadFile(localFile, surveyId)
    }

    /**
     * Uploads or updates a FirestoreSurvey document in Cloud Firestore
     */
    suspend fun uploadSurveyDocument(survey: FirestoreSurvey) {
        firestoreDb.collection("surveys")
            .document(survey.surveyId)
            .set(survey)
            .await()
    }

    /**
     * Fetches all surveys from Cloud Firestore
     */
    suspend fun fetchAllSurveys(): List<FirestoreSurvey> {
        val snapshot = firestoreDb.collection("surveys").get().await()
        return snapshot.documents.mapNotNull { it.toObject(FirestoreSurvey::class.java) }
    }
}