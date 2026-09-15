package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ConverterViewModel
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.config.ConvertConfigScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.progress.ConvertProgressScreen
import com.example.ui.screens.result.ConvertResultScreen
import com.example.ui.screens.select.SelectAudioScreen
import com.example.ui.theme.AudioStudioTheme

/**
 * Actividad principal de AudioStudio.
 *
 * Configura la navegación de múltiples pantallas entre el hub principal,
 * selector de audio, configuración de bireraje/formato, progreso, resultado e historial.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioStudioTheme {
                AudioStudioApp()
            }
        }
    }
}

@Composable
fun AudioStudioApp() {
    val navController = rememberNavController()
    val viewModel: ConverterViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Pantalla de inicio / Hub principal
        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSelectAudio = {
                    navController.navigate(NavRoutes.SELECT_AUDIO)
                },
                onNavigateToConfig = {
                    navController.navigate(NavRoutes.CONVERT_CONFIG)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                }
            )
        }

        // 2. Pantalla de selección de audio
        composable(NavRoutes.SELECT_AUDIO) {
            SelectAudioScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConfig = {
                    navController.navigate(NavRoutes.CONVERT_CONFIG)
                }
            )
        }

        // 3. Pantalla de configuración de parámetros (bitrate, formato, sample rate, canales)
        composable(NavRoutes.CONVERT_CONFIG) {
            ConvertConfigScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartConversion = {
                    navController.navigate(NavRoutes.CONVERT_PROGRESS)
                    viewModel.startConversion {
                        navController.navigate(NavRoutes.CONVERT_RESULT) {
                            popUpTo(NavRoutes.CONVERT_CONFIG) { inclusive = false }
                        }
                    }
                }
            )
        }

        // 4. Pantalla de progreso en tiempo real
        composable(NavRoutes.CONVERT_PROGRESS) {
            ConvertProgressScreen(
                viewModel = viewModel,
                onCancelled = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Pantalla de resultados y métricas de compresión
        composable(NavRoutes.CONVERT_RESULT) {
            ConvertResultScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateHistory = {
                    navController.navigate(NavRoutes.HISTORY) {
                        popUpTo(NavRoutes.HOME) { inclusive = false }
                    }
                }
            )
        }

        // 6. Pantalla de historial de conversiones guardadas en Room
        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartNewConversion = {
                    navController.navigate(NavRoutes.SELECT_AUDIO)
                }
            )
        }
    }
}
