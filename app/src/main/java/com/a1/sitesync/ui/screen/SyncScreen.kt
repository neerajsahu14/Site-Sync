package com.a1.sitesync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a1.sitesync.ui.viewmodel.FirebaseSyncViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Sync Screen: Sync status / history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBack: () -> Unit,
    viewModel: FirebaseSyncViewModel = koinViewModel()
) {
    val inProgress = viewModel.syncInProgress.collectAsState()
    val lastTime = viewModel.lastSyncTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Sync in progress: ${inProgress.value}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Last sync: ${lastTime.value?.toString() ?: "Never"}")
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.performSync() },
                    enabled = !inProgress.value
                ) {
                    Text(if (inProgress.value) "Syncing..." else "Sync Now")
                }
            }
        }
    )
}
