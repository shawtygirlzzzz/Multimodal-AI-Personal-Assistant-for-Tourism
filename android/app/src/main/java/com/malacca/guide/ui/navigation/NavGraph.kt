package com.malacca.guide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.malacca.guide.ui.screens.HomeScreen
import com.malacca.guide.ui.screens.ListeningScreen
import com.malacca.guide.ui.screens.LoadingScreen
import com.malacca.guide.ui.screens.ResultScreen
import com.malacca.guide.ui.screens.SplashScreen
import com.malacca.guide.ui.viewmodel.GuideViewModel
import com.malacca.guide.voice.TtsManager

const val ROUTE_SPLASH    = "splash"
const val ROUTE_HOME      = "home"
const val ROUTE_LISTENING = "listening"
const val ROUTE_LOADING   = "loading"
const val ROUTE_RESULT    = "result"

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val viewModel: GuideViewModel = viewModel()
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    NavHost(navController = navController, startDestination = ROUTE_SPLASH) {
        composable(ROUTE_SPLASH)    { SplashScreen(navController) }
        composable(ROUTE_HOME)      { HomeScreen(navController, viewModel) }
        composable(ROUTE_LISTENING) { ListeningScreen(navController, viewModel) }
        composable(ROUTE_LOADING)   { LoadingScreen(navController, viewModel) }
        composable(ROUTE_RESULT)    { ResultScreen(navController, viewModel, ttsManager) }
    }
}
