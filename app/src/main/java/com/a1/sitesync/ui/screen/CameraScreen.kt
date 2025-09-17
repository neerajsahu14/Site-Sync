package com.a1.sitesync.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNext: (String) -> Unit,
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Camera Logic ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    // The URI from TakePicture is the one we created, pointing to our file.
                    // We just need to get the path from the URI we stored.
                    val file = context.getFileFromUri(uri)
                    viewModel.addPhotoToSurvey(surveyId, file.absolutePath)
                    onNext(surveyId)
                }
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val newUri = context.createImageFileUri(surveyId)
                tempImageUri = newUri
                cameraLauncher.launch(newUri)
            } else {
                // Handle permission denial gracefully, e.g., show a snackbar.
            }
        }
    )

    // --- Gallery Logic ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val newFile = context.createFileFromContentUri(it, surveyId)
                viewModel.addPhotoToSurvey(surveyId, newFile.absolutePath)
                onNext(surveyId)
            }
        }
    )

    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                galleryLauncher.launch("image/*")
            } else {
                // Handle permission denial gracefully.
            }
        }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add a Photo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }) {
                Text("Take Photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                galleryPermissionLauncher.launch(galleryPermission)
            }) {
                Text("Select from Gallery")
            }
        }
    }
}

// --- Helper Functions ---

private fun Context.createImageFileUri(surveyId: String): Uri {
    val file = createImageFile(surveyId)
    return FileProvider.getUriForFile(
        Objects.requireNonNull(this),
        "${this.packageName}.provider",
        file
    )
}

private fun Context.createImageFile(surveyId: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "IMG_${surveyId}_${timeStamp}.jpg"
    return File(this.filesDir, imageFileName)
}

private fun Context.getFileFromUri(uri: Uri): File {
    // This is a simplified way for FileProvider URIs. A more robust solution
    // would handle different URI schemes.
    return File(this.filesDir, uri.path!!.substringAfterLast('/'))
}

private fun Context.createFileFromContentUri(contentUri: Uri, surveyId: String): File {
    val newFile = createImageFile(surveyId)
    val inputStream = this.contentResolver.openInputStream(contentUri)
    val outputStream = FileOutputStream(newFile)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    return newFile
}
