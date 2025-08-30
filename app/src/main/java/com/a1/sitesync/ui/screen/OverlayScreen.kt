package com.a1.sitesync.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Overlay Screen: Place gate design overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(
    onNext: (String) -> Unit,
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val surveyState by viewModel.getSurvey(surveyId).collectAsState(initial = null)
    val loaded = surveyState
    // Sample overlay designs resource IDs, replace with actual drawables
    val designs = listOf<String>("Design A", "Design B", "Design C")
    var selected by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Overlay Photo") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                loaded?.photos?.lastOrNull()?.localFilePath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    if (selected != null) {
                        // placeholder for overlay
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color(0x550000FF))
                        ) {
                            Text(selected!!, color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
                Text("Select Overlay Design:")
                LazyRow { items(designs) { d ->
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { selected = d }
                    ) {
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            Text(d, fontSize = 12.sp)
                        }
                    }
                } }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { onNext(surveyId) }, enabled = selected != null) {
                    Text("Save Overlay")
                }
            }
        }
    )
}
