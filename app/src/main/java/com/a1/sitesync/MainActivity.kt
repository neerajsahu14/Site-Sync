package com.a1.sitesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.a1.sitesync.ui.theme.SiteSyncTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.a1.sitesync.ui.screen.CameraScreen
import com.a1.sitesync.ui.screen.FormScreen
import com.a1.sitesync.ui.screen.OverlayScreen
import com.a1.sitesync.ui.screen.PreviewScreen
import com.a1.sitesync.ui.screen.SyncScreen
import com.a1.sitesync.ui.screen.SurveyListScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiteSyncTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SiteSyncNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SiteSyncNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = modifier
    ) {
        composable("list") {
            SurveyListScreen(
                onAddNew = { navController.navigate("form/new") },
                onItemClick = { id -> navController.navigate("form/$id") }
            )
        }
        composable("form") { FormScreen(onNext = { navController.navigate("camera") }) }
        composable(
            route = "form/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            FormScreen(onNext = { id -> navController.navigate("camera/$id") }, surveyId = surveyId)
        }
        composable(
            route = "camera/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId") ?: ""
            CameraScreen(onNext = { id -> navController.navigate("overlay/$id") }, surveyId = surveyId)
        }
        composable(
            route = "overlay/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId") ?: ""
            OverlayScreen(onNext = { id -> navController.navigate("preview/$id") }, surveyId = surveyId)
        }
        composable(
            route = "preview/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId") ?: ""
            PreviewScreen(onNext = { navController.navigate("sync") }, surveyId = surveyId)
        }
        composable("sync") { SyncScreen(onBack = { navController.popBackStack() }) }
    }
}