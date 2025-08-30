package com.a1.sitesync.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a1.sitesync.data.database.model.Dimensions
import com.a1.sitesync.data.database.model.Provisions
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import com.a1.sitesync.R

/**
 * Survey Form Screen: Input site dimensions + cabling/storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNext: (String) -> Unit,
    surveyId: String? = null,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val editing = surveyId != null && surveyId != "new"
    val surveyState = if (editing) viewModel.getSurvey(surveyId!!).collectAsState(initial = null) else null
    val loaded = surveyState?.value

    // form fields state
    var clientName by remember { mutableStateOf(TextFieldValue(loaded?.survey?.clientName ?: "")) }
    var siteAddress by remember { mutableStateOf(TextFieldValue(loaded?.survey?.siteAddress ?: "")) }
    var gateType by remember { mutableStateOf(loaded?.survey?.gateType ?: "Swing") }
    var clearWidth by remember { mutableStateOf(loaded?.survey?.dimensions?.clearOpeningWidth?.toString() ?: "") }
    var reqHeight by remember { mutableStateOf(loaded?.survey?.dimensions?.requiredHeight?.toString() ?: "") }
    var parkingLen by remember { mutableStateOf(loaded?.survey?.dimensions?.parkingSpaceLength?.toString() ?: "") }
    var openingAngle by remember { mutableStateOf(loaded?.survey?.dimensions?.openingAngleLeaf?.toString() ?: "") }
    var hasCabling by remember { mutableStateOf(loaded?.survey?.provisions?.hasCabling ?: false) }
    var hasStorage by remember { mutableStateOf(loaded?.survey?.provisions?.hasStorage ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Survey" else "New Survey") },
                actions = {
                    TextButton(onClick = {
                        // save
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
                                    provisions = prov
                                )
                                viewModel.updateSurvey(s)
                            }
                        } else {
                            viewModel.createSurvey(
                                surveyorId = "", clientName.text, siteAddress.text,
                                gateType, dims, prov,
                                openingDirection = null, recommendedGate = null,
                                photoPaths = emptyList()
                            )
                        }
                    }) { Text("Save") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val id = surveyId ?: "new"
                onNext(id)
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
                // Client Info
                TextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = siteAddress,
                    onValueChange = { siteAddress = it },
                    label = { Text("Site Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Gate Type Selection
                Text("Gate Type:")
                Row {
                    listOf("Swing", "Sliding").forEach { type ->
                        FilterChip(
                            selected = gateType == type,
                            onClick = { gateType = type },
                            label = { Text(type) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                // Dimensions
                TextField(
                    value = clearWidth,
                    onValueChange = { clearWidth = it },
                    label = { Text("Clear Opening Width") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = reqHeight,
                    onValueChange = { reqHeight = it },
                    label = { Text("Required Height") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (gateType == "Sliding") {
                    TextField(
                        value = parkingLen,
                        onValueChange = { parkingLen = it },
                        label = { Text("Parking Space Length") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextField(
                        value = openingAngle,
                        onValueChange = { openingAngle = it },
                        label = { Text("Opening Angle Leaf") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Provisions
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
                // Photos
                loaded?.photos?.let { photos ->
                    Text("Photos:")
                    LazyRow { items(photos) { p ->
                        // display thumbnail placeholder
                        Box(modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.surface)
                        )
                    } }
                }
            }
        }
    )
}
