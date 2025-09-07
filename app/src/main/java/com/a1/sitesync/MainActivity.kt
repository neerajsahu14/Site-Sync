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
import com.a1.sitesync.ui.screen.DetailsScreen
import com.a1.sitesync.ui.screen.FormScreen
import com.a1.sitesync.ui.screen.OverlayScreen
import com.a1.sitesync.ui.screen.PreviewScreen
import com.a1.sitesync.ui.screen.SyncScreen
import com.a1.sitesync.ui.screen.SurveyListScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.a1.sitesync.data.repository.AuthRepository
import com.a1.sitesync.ui.screen.AuthScreen
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiteSyncTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SiteSyncNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}

@Composable
fun SiteSyncNavHost(
    navController: NavHostController, 
    modifier: Modifier = Modifier,
    authRepository: AuthRepository
) {
    val startDestination = if (authRepository.getCurrentUser() != null) "list" else "auth"
    val siteSyncViewModel: SiteSyncViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("auth") {
            AuthScreen(onSignInSuccess = { navController.navigate("list") { popUpTo("auth") { inclusive = true } } })
        }
        composable("list") {
            SurveyListScreen(
                onAddNew = { navController.navigate("form/new") },
                onItemClick = { id -> navController.navigate("details/$id") },
                onSyncClick = { surveyId -> siteSyncViewModel.syncSurveyById(surveyId) }
            )
        }
        composable("form") { 
            FormScreen(
                onNext = { navController.navigate("camera") },
                onSaveComplete = { navController.popBackStack() }
            ) 
        }
        composable(
            route = "form/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            FormScreen(
                onNext = { id -> navController.navigate("camera/$id") }, 
                surveyId = surveyId,
                onSaveComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = "details/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            requireNotNull(surveyId) { "surveyId parameter wasn't found. Please make sure it's set!" }
            DetailsScreen(surveyId = surveyId)
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