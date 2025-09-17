package com.a1.sitesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.a1.sitesync.data.repository.AuthRepository
import com.a1.sitesync.ui.screen.*
import com.a1.sitesync.ui.theme.SiteSyncTheme
import com.a1.sitesync.ui.viewmodel.SiteSyncViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiteSyncTheme {
                val navController = rememberNavController()
                SiteSyncNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    authRepository = authRepository
                )
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

    if (startDestination == "list") {
        LaunchedEffect(Unit) {
            siteSyncViewModel.syncData()
        }
    }

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
                onEditClick = { id -> navController.navigate("form/$id") }
            )
        }
        composable("form") {
            FormScreen(
                surveyId = null,
                onSaveComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = "form/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            FormScreen(
                surveyId = surveyId,
                onSaveComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = "details/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            requireNotNull(surveyId) { "surveyId parameter wasn't found." }
            DetailsScreen(
                surveyId = surveyId,
                onModifyClick = { id -> navController.navigate("form/$id") },
                onPhotoClick = { sId, pId -> navController.navigate("overlay/$sId/$pId") },
                onOverlaidPhotoClick = { paths, index ->
                    val encodedPaths = URLEncoder.encode(paths.joinToString(","), "UTF-8")
                    navController.navigate("fullScreenViewer/$encodedPaths/$index")
                }
            )
        }
        composable(
            route = "camera/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId") ?: ""
            CameraScreen(
                surveyId = surveyId,
                onNext = { sId -> navController.navigate("details/$sId") { popUpTo("list") } }
            )
        }
        composable(
            route = "overlay/{surveyId}/{photoId}",
            arguments = listOf(
                navArgument("surveyId") { type = NavType.StringType },
                navArgument("photoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString("surveyId")
            val photoId = backStackEntry.arguments?.getString("photoId")
            requireNotNull(surveyId) { "surveyId parameter wasn't found." }
            requireNotNull(photoId) { "photoId parameter wasn't found." }
            OverlayScreen(
                surveyId = surveyId,
                photoId = photoId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "fullScreenViewer/{imagePaths}/{initialPage}",
            arguments = listOf(
                navArgument("imagePaths") { type = NavType.StringType },
                navArgument("initialPage") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val imagePathsStr = backStackEntry.arguments?.getString("imagePaths") ?: ""
            val initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0
            val decodedPaths = URLDecoder.decode(imagePathsStr, "UTF-8").split(",")
            FullScreenImageViewer(
                imagePaths = decodedPaths,
                initialPage = initialPage,
                onBack = { navController.popBackStack() }
            )
        }
    }
}