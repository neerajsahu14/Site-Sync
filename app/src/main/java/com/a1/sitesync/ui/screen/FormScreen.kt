package com.a1.sitesync.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import coil.compose.rememberAsyncImagePainter
import com.a1.sitesync.R
import com.a1.sitesync.data.database.model.Dimensions
import com.a1.sitesync.data.database.model.Provisions
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNext: (String) -> Unit,
    onSaveComplete: () -> Unit,
    surveyId: String? = null,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val editing = surveyId != null && surveyId != "new"
    val surveyState = if (editing) viewModel.getSurvey(surveyId!!).collectAsState(initial = null) else null
    val loaded = surveyState?.value
    val context = LocalContext.current

    // Form fields state
    var clientName by remember { mutableStateOf(TextFieldValue("")) }
    var siteAddress by remember { mutableStateOf(TextFieldValue("")) }
    var gateType by remember { mutableStateOf("Swing") }
    var clearWidth by remember { mutableStateOf("") }
    var reqHeight by remember { mutableStateOf("") }
    var parkingLen by remember { mutableStateOf("") }
    var openingAngle by remember { mutableStateOf("") }
    var hasCabling by remember { mutableStateOf(false) }
    var hasStorage by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            latitude = it.latitude.toString()
                            longitude = it.longitude.toString()
                        }
                    }
                }
            }
        }
    )

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
            latitude = loaded.survey.latitude?.toString() ?: ""
            longitude = loaded.survey.longitude?.toString() ?: ""
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Survey" else "New Survey") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val dims = Dimensions(
                                clearWidth.toDoubleOrNull() ?: 0.0,
                                reqHeight.toDoubleOrNull() ?: 0.0,
                                if (gateType == "Sliding") parkingLen.toDoubleOrNull() else null,
                                if (gateType == "Swing") openingAngle.toIntOrNull() else null
                            )
                            val prov = Provisions(hasCabling, hasStorage)
                            if (editing) {
                                loaded?.let {
                                    val s = it.survey.copy(
                                        clientName = clientName.text,
                                        siteAddress = siteAddress.text,
                                        gateType = gateType,
                                        dimensions = dims,
                                        provisions = prov,
                                        latitude = latitude.toDoubleOrNull(),
                                        longitude = longitude.toDoubleOrNull()
                                    )
                                    viewModel.updateSurvey(s)
                                }
                            } else {
                                viewModel.createSurvey(
                                    surveyorId = "", clientName.text, siteAddress.text,
                                    gateType, dims, prov,
                                    openingDirection = null, recommendedGate = null,
                                    photoPaths = emptyList(),
                                    latitude = latitude.toDoubleOrNull(),
                                    longitude = longitude.toDoubleOrNull()
                                )
                            }
                            onSaveComplete()
                        }
                    }) { Text("Save") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                coroutineScope.launch {
                    val id = if (editing) surveyId!! else {
                        val dims = Dimensions(
                            clearWidth.toDoubleOrNull() ?: 0.0,
                            reqHeight.toDoubleOrNull() ?: 0.0,
                            if (gateType == "Sliding") parkingLen.toDoubleOrNull() else null,
                            if (gateType == "Swing") openingAngle.toIntOrNull() else null
                        )
                        val prov = Provisions(hasCabling, hasStorage)
                        viewModel.createSurvey(
                            surveyorId = "", // TODO: Pass real surveyorId if available
                            clientName = clientName.text,
                            siteAddress = siteAddress.text,
                            gateType = gateType,
                            dimensions = dims,
                            provisions = prov,
                            openingDirection = null,
                            recommendedGate = null,
                            photoPaths = emptyList(),
                            latitude = latitude.toDoubleOrNull(),
                            longitude = longitude.toDoubleOrNull()
                        )
                    }
                    onNext(id)
                }
            }) {
                Icon(painterResource(R.drawable.baseline_add_photo_alternate_24), contentDescription = "Capture Photo")
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ... (rest of the form fields)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Location:")
                    Button(onClick = { 
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                             fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let {
                                    latitude = it.latitude.toString()
                                    longitude = it.longitude.toString()
                                }
                            }
                        }
                    }) {
                        Text("Get Location")
                    }
                }
                TextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                TextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                // Photos
                loaded?.photos?.let { photos ->
                    Text("Photos:")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(photos) { p ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = p.localFilePath,
                                placeholder = painterResource(id = R.drawable.baseline_image_24),
                                error = painterResource(id = R.drawable.baseline_broken_image_24)
                            ),
                            contentDescription = "Captured photo",
                            modifier = Modifier
                                .size(120.dp)
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Crop
                        )
                    } }
                }
            }
        }
    )
}
