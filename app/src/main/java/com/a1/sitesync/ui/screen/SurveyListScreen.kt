package com.a1.sitesync.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import com.a1.sitesync.R

/**
 * Survey List Screen: Shows all surveys and allows starting new ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyListScreen(
    onAddNew: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val surveys = viewModel.surveys.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SiteSync Surveys") },
                actions = {
                    IconButton(onClick = { viewModel.syncData() }) {
                        Icon(painterResource(R.drawable.baseline_cloud_queue_24), contentDescription = "Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "New Survey")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(surveys.value) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onItemClick(item.survey.surveyId) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.survey.clientName)
                            Text(
                                item.survey.siteAddress ?: "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            Text(
                                sdf.format(item.survey.createdAt),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (item.survey.isSynced) {
                            Icon(painterResource(R.drawable.baseline_cloud_done_24), contentDescription = "Synced")
                        } else {
                            Icon(painterResource(R.drawable.baseline_cloud_off_24), contentDescription = "Unsynced")
                        }
                    }
                }
            }
        }
    }
}
