package com.a1.sitesync.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

@Composable
fun CameraScreen(
    onNext: (String) -> Unit,
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var hasImage by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            hasImage = success
            if (success) {
                imageFile?.let {
                    viewModel.addPhotoToSurvey(surveyId, it.absolutePath)
                    onNext(surveyId)
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            val file = context.createImageFile(surveyId)
            val uri = FileProvider.getUriForFile(
                Objects.requireNonNull(context),
                "com.a1.sitesync.provider", // Make sure this matches your FileProvider authority
                file
            )
            imageFile = file
            imageUri = uri
            cameraLauncher.launch(uri)
        }) {
            Text("Open Camera")
        }
    }
}

private fun Context.createImageFile(surveyId: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "IMG_${surveyId}_${timeStamp}"
    return File(filesDir, "$imageFileName.jpg")
}