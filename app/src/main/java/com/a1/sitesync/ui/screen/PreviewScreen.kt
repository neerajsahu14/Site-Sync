package com.a1.sitesync.ui.screen

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.core.content.FileProvider
import androidx.compose.material3.CircularProgressIndicator

/**
 * Preview Screen: Preview + Generate PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onNext: () -> Unit,
    surveyId: String,
    viewModel: SiteSyncViewModel = koinViewModel()
) {
    val context = LocalContext.current
    // Trigger PDF generation on enter
    LaunchedEffect(surveyId) {
        viewModel.generateReport(context, surveyId)
    }
    val reportPath by viewModel.reportFilePath.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Report Preview") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (reportPath == null) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating report...")
                } else {
                    Text("Report ready:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(reportPath ?: "", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            // Open PDF
                            try {
                                val file = File(reportPath!!)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                // Handle case where no PDF viewer is installed
                            }
                        }) { Text("View") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            // Share PDF
                            try {
                                val file = File(reportPath!!)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share Report"
                                    )
                                )
                            } catch (e: Exception) {
                                // Handle exceptions
                            }
                        }) { Text("Share") }
                    }
                }
            }
        }
    )
}
