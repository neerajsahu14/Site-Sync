package com.a1.sitesync.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.a1.sitesync.R
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val surveyState = viewModel.getSurvey(surveyId).collectAsState(initial = null)
    val surveyWithPhotos = surveyState.value
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val reportFilePath by viewModel.reportFilePath.collectAsState()

    // Show snackbar when report is generated
    LaunchedEffect(reportFilePath) {
        reportFilePath?.let {
            val result = snackbarHostState.showSnackbar(
                message = "Report saved to $it",
                actionLabel = "Open"
            )
            if (result == SnackbarResult.ActionPerformed) {
                openPdf(context, it)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Survey Details") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.generateReport(context, surveyId)
            }) {
                Icon(painterResource(id = R.drawable.outline_docs_24), contentDescription = "Generate PDF")
            }
        }
    ) {
        if (surveyWithPhotos != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Client: ${surveyWithPhotos.survey.clientName}", style = MaterialTheme.typography.headlineSmall)
                Text("Address: ${surveyWithPhotos.survey.siteAddress}", style = MaterialTheme.typography.bodyLarge)
                Text("Gate Type: ${surveyWithPhotos.survey.gateType}", style = MaterialTheme.typography.bodyLarge)
                
                // Dimensions
                Text("Dimensions", style = MaterialTheme.typography.titleMedium)
                Text("Clear Opening Width: ${surveyWithPhotos.survey.dimensions.clearOpeningWidth} m")
                Text("Required Height: ${surveyWithPhotos.survey.dimensions.requiredHeight} m")
                surveyWithPhotos.survey.dimensions.parkingSpaceLength?.let {
                    Text("Parking Space Length: $it m")
                }
                surveyWithPhotos.survey.dimensions.openingAngleLeaf?.let {
                    Text("Opening Angle: $it degrees")
                }

                // Provisions
                Text("Provisions", style = MaterialTheme.typography.titleMedium)
                Text("Cabling: ${if (surveyWithPhotos.survey.provisions.hasCabling) "Yes" else "No"}")
                Text("Storage: ${if (surveyWithPhotos.survey.provisions.hasStorage) "Yes" else "No"}")

                // Photos
                if (surveyWithPhotos.photos.isNotEmpty()) {
                    Text("Photos", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(surveyWithPhotos.photos) { photo ->
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = photo.localFilePath,
                                    placeholder = painterResource(id = R.drawable.baseline_image_24),
                                    error = painterResource(id = R.drawable.baseline_broken_image_24)
                                ),
                                contentDescription = "Captured photo",
                                modifier = Modifier.size(150.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun openPdf(context: Context, filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(context, "com.a1.sitesync.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
