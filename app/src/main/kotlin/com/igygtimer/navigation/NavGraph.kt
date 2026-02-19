package com.igygtimer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = { config ->
                    viewModel.startWorkout(config)
                    TimerService.startService(context)
                    navController.navigate(Routes.TIMER)
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
