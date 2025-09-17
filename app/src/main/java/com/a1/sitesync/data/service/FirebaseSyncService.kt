package com.a1.sitesync.data.service

import android.content.Context
import android.net.Uri
import com.a1.sitesync.data.models.FirestoreSurvey
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class FirebaseSyncService(
    private val context: Context,
    private val storage: FirebaseStorage
) {
    private val firestoreDb = FirebaseFirestore.getInstance()

    suspend fun uploadFile(fileUri: Uri, surveyId: String, fileName: String): String {
        val storageRef = storage.reference.child("surveys/$surveyId/$fileName")
        val uploadTask = storageRef.putFile(fileUri).await()
        return uploadTask.storage.downloadUrl.await().toString()
    }

    /**
     * Downloads a file from the given Cloud Storage URL to local app storage.
     *
     * @param fileUrl The public URL of the file to download.
     * @return The local File object where the file was saved.
     */
    suspend fun downloadFile(fileUrl: String): File {
        val httpsReference = storage.getReferenceFromUrl(fileUrl)
        val localFile = File(context.filesDir, "${UUID.randomUUID()}.jpg")
        httpsReference.getFile(localFile).await()
        return localFile
    }

    suspend fun uploadSurveyDocument(survey: FirestoreSurvey) {
        firestoreDb.collection("surveys")
            .document(survey.surveyId)
            .set(survey)
            .await()
    }

    suspend fun fetchAllSurveys(): List<FirestoreSurvey> {
        val snapshot = firestoreDb.collection("surveys").get().await()
        return snapshot.documents.mapNotNull { it.toObject(FirestoreSurvey::class.java) }
    }
}