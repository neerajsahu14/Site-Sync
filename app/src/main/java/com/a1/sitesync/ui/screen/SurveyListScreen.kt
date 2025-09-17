package com.a1.sitesync.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a1.sitesync.R
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyListScreen(
    onAddNew: () -> Unit,
    onItemClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val surveys by viewModel.surveys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SiteSync Surveys") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "New Survey")
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding)
        ) {
            items(surveys) { item ->
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onItemClick(item.survey.surveyId) },
                                    onLongPress = { showMenu = true }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.survey.clientName)
                                Text(item.survey.siteAddress ?: "", style = MaterialTheme.typography.bodySmall)
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                Text(sdf.format(item.survey.createdAt), style = MaterialTheme.typography.bodySmall)
                            }
                            if (item.survey.isSynced) {
                                Icon(painterResource(R.drawable.baseline_cloud_done_24), contentDescription = "Synced")
                            } else {
                                Icon(painterResource(R.drawable.baseline_cloud_off_24), contentDescription = "Unsynced")
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditClick(item.survey.surveyId)
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Loading indicator at the bottom
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        // Effect to trigger loading more items
        val shouldLoadMore = remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }

        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) {
                viewModel.loadMoreSurveys()
            }
        }
    }
}
