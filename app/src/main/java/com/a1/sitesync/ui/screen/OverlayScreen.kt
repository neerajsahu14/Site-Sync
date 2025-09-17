package com.a1.sitesync.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.a1.sitesync.data.models.Gate
import com.a1.sitesync.ui.viewmodel.OverlayViewModel
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(
    surveyId: String,
    photoId: String,
    onBack: () -> Unit,
    siteSyncViewModel: SiteSyncViewModel = koinViewModel(),
    overlayViewModel: OverlayViewModel = koinViewModel()
) {
    val surveyState by siteSyncViewModel.getSurvey(surveyId).collectAsState(initial = null)
    val gates by overlayViewModel.gates.collectAsState()
    val selectedGate by overlayViewModel.selectedGate.collectAsState()

    val offset by overlayViewModel.overlayOffset.collectAsState()
    val scale by overlayViewModel.overlayScale.collectAsState()
    val rotation by overlayViewModel.overlayRotation.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundPhoto = remember(surveyState, photoId) {
        surveyState?.photos?.find { it.photoId == photoId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design Your Gate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { overlayViewModel.resetTransformations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            if (backgroundPhoto != null && selectedGate != null) {
                                val resultFile = createOverlayedImage(
                                    context = context,
                                    backgroundPath = backgroundPhoto.localFilePath,
                                    overlayDrawableId = selectedGate!!.imageResourceId,
                                    offset = offset,
                                    scale = scale,
                                    rotation = rotation
                                )
                                siteSyncViewModel.addPhotoToSurvey(surveyId, resultFile.absolutePath, isSuperimposed = true)
                                onBack()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Overlay")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Main content area for image and overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, gestureRotation ->
                            overlayViewModel.onOverlayDrag(pan)
                            overlayViewModel.onOverlayZoom(zoom)
                            overlayViewModel.onOverlayRotate(gestureRotation)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Image
                backgroundPhoto?.let {
                    Image(
                        painter = rememberAsyncImagePainter(model = it.localFilePath),
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Gate Overlay Image
                selectedGate?.let {
                    Image(
                        painter = painterResource(id = it.imageResourceId),
                        contentDescription = "Gate Overlay",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                rotationZ = rotation,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // Gate Selection Carousel at the bottom
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(gates) { gate ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { overlayViewModel.onGateSelected(gate) }
                            .border(
                                width = 2.dp,
                                color = if (selectedGate?.name == gate.name) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = gate.imageResourceId),
                            contentDescription = gate.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

private suspend fun createOverlayedImage(
    context: Context,
    backgroundPath: String,
    @DrawableRes overlayDrawableId: Int,
    offset: androidx.compose.ui.geometry.Offset,
    scale: Float,
    rotation: Float
): File {
    val imageLoader = ImageLoader(context)

    // Load background bitmap
    val backgroundRequest = ImageRequest.Builder(context).data(File(backgroundPath)).allowHardware(false).build()
    val backgroundDrawable = imageLoader.execute(backgroundRequest).drawable
    val backgroundBitmap = (backgroundDrawable as BitmapDrawable).bitmap.copy(Bitmap.Config.ARGB_8888, true)

    // Load overlay bitmap from drawable resources
    val overlayRequest = ImageRequest.Builder(context).data(overlayDrawableId).allowHardware(false).build()
    val overlayDrawable = imageLoader.execute(overlayRequest).drawable
    val overlayBitmap = (overlayDrawable as BitmapDrawable).bitmap

    // Create a Canvas to draw on the background bitmap
    val canvas = Canvas(backgroundBitmap)

    // Calculate the initial position to center the overlay
    val initialX = (canvas.width - overlayBitmap.width) / 2f
    val initialY = (canvas.height - overlayBitmap.height) / 2f

    // Apply transformations
    canvas.save()
    canvas.translate(initialX + offset.x, initialY + offset.y)
    canvas.rotate(rotation, overlayBitmap.width / 2f, overlayBitmap.height / 2f)
    canvas.scale(scale, scale, overlayBitmap.width / 2f, overlayBitmap.height / 2f)
    canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
    canvas.restore()

    // Save the file to the external cache directory to match FileProvider paths
    val file = File(context.externalCacheDir, "OVERLAY_${UUID.randomUUID()}.jpg")
    FileOutputStream(file).use {
        backgroundBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
    }
    return file
}
