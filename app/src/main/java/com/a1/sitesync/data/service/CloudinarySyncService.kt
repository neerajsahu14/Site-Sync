package com.a1.sitesync.data.service

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.collections.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A dedicated service to handle all file upload communications with Cloudinary.
 * This isolates Cloudinary's SDK and logic from the main repository.
 */
class CloudinarySyncService {

    /**
     * Uploads a single file to your Cloudinary cloud.
     *
     * This function is built to be coroutine-friendly by wrapping Cloudinary's
     * callback-based upload API in a suspendCancellableCoroutine.
     *
     * @param localFile The local file to be uploaded.
     * @param surveyId The unique ID of the survey to create a unique public_id.
     * @return The secure public URL of the uploaded file as a String.
     * @throws Exception if the upload fails for any reason.
     */
    suspend fun uploadFile(localFile: File, surveyId: String): String {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(Uri.fromFile(localFile))
                .option("public_id", "sitesync/surveys/$surveyId/${localFile.name}")
                .option("resource_type", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Optional: Log or handle upload start
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Optional: Handle progress updates
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        // On success, resume the coroutine with the secure URL
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(Exception("Cloudinary upload succeeded but URL was not returned."))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        // On error, resume the coroutine with an exception
                        continuation.resumeWithException(Exception("Cloudinary upload failed: ${error.description}"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Optional: Handle rescheduling
                    }
                }).dispatch()

            // Handle cancellation of the coroutine
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}