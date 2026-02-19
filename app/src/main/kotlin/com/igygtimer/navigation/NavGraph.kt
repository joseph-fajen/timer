package com.igygtimer.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.service.TimerService
import com.igygtimer.ui.screen.CompleteScreen
import com.igygtimer.ui.screen.HomeScreen
import com.igygtimer.ui.screen.TimerScreen
import com.igygtimer.viewmodel.TimerViewModel

object Routes {
    const val HOME = "home"
    const val TIMER = "timer"
    const val COMPLETE = "complete"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: TimerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var pendingConfig by remember { mutableStateOf<WorkoutConfig?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Start workout regardless of permission result (service still works, just no notification)
        pendingConfig?.let { config ->
            viewModel.startWorkout(config)
            TimerService.startService(context)
            navController.navigate(Routes.TIMER)
            pendingConfig = null
        }
    }

    fun startWorkoutWithPermissionCheck(config: WorkoutConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                pendingConfig = config
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        // Permission granted or not needed (API < 33)
        viewModel.startWorkout(config)
        TimerService.startService(context)
        navController.navigate(Routes.TIMER)
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = { config ->
                    startWorkoutWithPermissionCheck(config)
                }
            )
        }

        composable(Routes.TIMER) {
            TimerScreen(
                uiState = uiState,
                onWorkDone = { viewModel.onWorkDone() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onStop = {
                    viewModel.stop()
                    TimerService.stopService(context)
                    navController.popBackStack(Routes.HOME, false)
                },
                onComplete = {
                    navController.navigate(Routes.COMPLETE) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.COMPLETE) {
            CompleteScreen(
                totalTimeMs = uiState.totalElapsedMs,
                totalRounds = uiState.totalRounds,
                onDone = {
                    viewModel.reset()
                    TimerService.stopService(context)
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}
