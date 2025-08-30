package com.a1.sitesync.ui.screen

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView

/**
 * Camera Screen: Capture site photos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNext: (String) -> Unit,
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val context = LocalContext.current
    // Request camera permission
    LaunchedEffect(Unit) {
        ActivityCompat.requestPermissions(
            context as android.app.Activity,
            arrayOf(Manifest.permission.CAMERA), 0
        )
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    AndroidView({ PreviewView(context) }) { previewView ->
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (_: Exception) {
                // handle error
            }
        }, ContextCompat.getMainExecutor(context))
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Capture Photo") }) },
        content = { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis())
                    val photoFile = File(
                        context.filesDir,
                        "IMG_${surveyId}_$timestamp.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.addPhotoToSurvey(surveyId, photoFile.absolutePath)
                                onNext(surveyId)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                // handle error
                            }
                        }
                    )
                }) {
                    Text("Capture")
                }
            }
        }
    )
}
