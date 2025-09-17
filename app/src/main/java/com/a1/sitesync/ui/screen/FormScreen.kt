package com.a1.sitesync.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.a1.sitesync.data.database.model.Dimensions
import com.a1.sitesync.data.database.model.Provisions
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onSaveComplete: () -> Unit,
    surveyId: String?,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val editing = surveyId != null && surveyId != "new"
    val surveyState by viewModel.getSurvey(surveyId ?: "").collectAsState(initial = null)
    val loaded = surveyState

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State Management ---
    var clientName by remember { mutableStateOf(TextFieldValue("")) }
    var siteAddress by remember { mutableStateOf(TextFieldValue("")) }
    var gateType by remember { mutableStateOf("Swing") }
    var clearWidth by remember { mutableStateOf("") }
    var reqHeight by remember { mutableStateOf("") }
    var parkingLen by remember { mutableStateOf("") }
    var openingAngle by remember { mutableStateOf("") }
    var hasCabling by remember { mutableStateOf(false) }
    var hasStorage by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- Permission & Activity Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let {
                val filePath = context.getFilePathFromUri(it)
                if (filePath != null && surveyId != null) {
                    viewModel.addPhotoToSurvey(surveyId, filePath)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageFile = context.createImageFile()
            val newImageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", newImageFile)
            tempCameraUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            // Handle permission denial gracefully in a real app (e.g., show a snackbar)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val filePath = context.copyUriToInternalStorage(it, "gallery_image")
            if (filePath != null && surveyId != null) {
                viewModel.addPhotoToSurvey(surveyId, filePath)
            }
        }
    }

    // --- Effects ---
    LaunchedEffect(loaded) {
        if (loaded != null) {
            clientName = TextFieldValue(loaded.survey.clientName)
            siteAddress = TextFieldValue(loaded.survey.siteAddress ?: "")
            gateType = loaded.survey.gateType
            clearWidth = loaded.survey.dimensions.clearOpeningWidth.toString()
            reqHeight = loaded.survey.dimensions.requiredHeight.toString()
            parkingLen = loaded.survey.dimensions.parkingSpaceLength?.toString() ?: ""
            openingAngle = loaded.survey.dimensions.openingAngleLeaf?.toString() ?: ""
            hasCabling = loaded.survey.provisions.hasCabling
            hasStorage = loaded.survey.provisions.hasStorage
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Survey" else "New Survey") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val dims = Dimensions(clearWidth.toDoubleOrNull() ?: 0.0, reqHeight.toDoubleOrNull() ?: 0.0, if (gateType == "Sliding") parkingLen.toDoubleOrNull() else null, if (gateType == "Swing") openingAngle.toIntOrNull() else null)
                            val prov = Provisions(hasCabling, hasStorage)
                            if (editing) {
                                loaded?.let {
                                    val s = it.survey.copy(clientName = clientName.text, siteAddress = siteAddress.text, gateType = gateType, dimensions = dims, provisions = prov)
                                    viewModel.updateSurvey(s)
                                }
                            } else {
                                val newSurveyId = viewModel.createSurvey(
                                    surveyorId = "", // Replace with actual surveyor ID
                                    clientName = clientName.text,
                                    siteAddress = siteAddress.text,
                                    gateType = gateType,
                                    dimensions = dims,
                                    provisions = prov,
                                    openingDirection = null,
                                    recommendedGate = null,
                                    photoPaths = emptyList(),
                                    latitude = null, // Add location capture logic if needed
                                    longitude = null
                                )
                                // This part is tricky without a proper state management for new surveys
                            }
                            onSaveComplete()
                        }
                    }) { Text("Save") }
                }
            )
        },
        floatingActionButton = {
            if (editing) {
                FloatingActionButton(onClick = { showImageSourceDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Photo")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, modifier = Modifier.fillMaxWidth())
            TextField(value = siteAddress, onValueChange = { siteAddress = it }, label = { Text("Site Address") }, modifier = Modifier.fillMaxWidth())
            Text("Gate Type:")
            Row {
                listOf("Swing", "Sliding").forEach { type ->
                    FilterChip(selected = gateType == type, onClick = { gateType = type }, label = { Text(type) })
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            TextField(value = clearWidth, onValueChange = { clearWidth = it }, label = { Text("Clear Opening Width") }, modifier = Modifier.fillMaxWidth())
            TextField(value = reqHeight, onValueChange = { reqHeight = it }, label = { Text("Required Height") }, modifier = Modifier.fillMaxWidth())
            if (gateType == "Sliding") {
                TextField(value = parkingLen, onValueChange = { parkingLen = it }, label = { Text("Parking Space Length") }, modifier = Modifier.fillMaxWidth())
            } else {
                TextField(value = openingAngle, onValueChange = { openingAngle = it }, label = { Text("Opening Angle Leaf") }, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Provision for Cabling")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = hasCabling, onCheckedChange = { hasCabling = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Provision for Storage")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = hasStorage, onCheckedChange = { hasStorage = it })
            }

            loaded?.photos?.let {
                Text("Photos:")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(it) { photo ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(model = photo.localFilePath),
                                contentDescription = "Captured photo",
                                modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.surface),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.deletePhotoById(photo.photoId) },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), shape = CircleShape).size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Choose Image Source") },
            text = { Text("Select a source for your image.") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Gallery")
                }
            }
        )
    }
}

private fun Context.createImageFile(): File {
    val timeStamp = UUID.randomUUID().toString()
    return File(filesDir, "IMG_$timeStamp.jpg")
}

private fun Context.copyUriToInternalStorage(uri: Uri, fileNamePrefix: String): String? {
    return try {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(filesDir, "${fileNamePrefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Context.getFilePathFromUri(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }
    // For content URIs, we need to copy the file to our app's storage
    return copyUriToInternalStorage(uri, "camera_image")
}
