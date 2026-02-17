package com.igygtimer.viewmodel

import app.cash.turbine.test
import com.igygtimer.model.TimerPhase
import com.igygtimer.model.WorkoutConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TimerViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.phase is TimerPhase.Idle)
            assertEquals(1, state.currentRound)
            assertEquals(10, state.totalRounds)
            assertEquals(1.0f, state.ratio)
        }
    }

    @Test
    fun `startWorkout transitions to Work phase`() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Skip initial Idle

            viewModel.startWorkout(WorkoutConfig(ratio = 1.5f, totalRounds = 5))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.phase is TimerPhase.Work)
            assertEquals(1, state.currentRound)
            assertEquals(5, state.totalRounds)
            assertEquals(1.5f, state.ratio)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause from Work transitions to Paused`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
            testDispatcher.scheduler.advanceUntilIdle()
            skipItems(1) // Skip Work state

            viewModel.pause()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.phase is TimerPhase.Paused)
            val paused = state.phase as TimerPhase.Paused
            assertTrue(paused.from is TimerPhase.Work)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stop resets to initial state`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
            testDispatcher.scheduler.advanceUntilIdle()
            skipItems(1)

            viewModel.stop()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.phase is TimerPhase.Idle)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `workout config is applied correctly`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.startWorkout(WorkoutConfig(ratio = 2.0f, totalRounds = 8))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(8, state.totalRounds)
            assertEquals(2.0f, state.ratio)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
