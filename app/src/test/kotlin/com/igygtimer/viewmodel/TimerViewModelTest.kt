package com.igygtimer.viewmodel

import com.igygtimer.model.TimerPhase
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTimeProvider = FakeTimeProvider(currentTimeMs = 1000L)
        viewModel = TimerViewModel(timeProvider = fakeTimeProvider)
    }

    @After
    fun tearDown() {
        // Cancel any running timer coroutines
        viewModel.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        val state = viewModel.uiState.value
        assertTrue(state.phase is TimerPhase.Idle)
        assertEquals(1, state.currentRound)
        assertEquals(10, state.totalRounds)
        assertEquals(1.0f, state.ratio)
    }

    @Test
    fun `startWorkout transitions to Work phase`() {
        viewModel.startWorkout(WorkoutConfig(ratio = 1.5f, totalRounds = 5))

        val state = viewModel.uiState.value
        assertTrue("Expected Work phase but got ${state.phase}", state.phase is TimerPhase.Work)
        assertEquals(1, state.currentRound)
        assertEquals(5, state.totalRounds)
        assertEquals(1.5f, state.ratio)
    }

    @Test
    fun `pause from Work transitions to Paused`() {
        viewModel.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        viewModel.pause()

        val state = viewModel.uiState.value
        assertTrue("Expected Paused phase but got ${state.phase}", state.phase is TimerPhase.Paused)
        val paused = state.phase as TimerPhase.Paused
        assertTrue("Expected paused from Work but got ${paused.from}", paused.from is TimerPhase.Work)
    }

    @Test
    fun `stop resets to initial state`() {
        viewModel.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        viewModel.stop()

        val state = viewModel.uiState.value
        assertTrue("Expected Idle phase but got ${state.phase}", state.phase is TimerPhase.Idle)
    }

    @Test
    fun `workout config is applied correctly`() {
        viewModel.startWorkout(WorkoutConfig(ratio = 2.0f, totalRounds = 8))

        val state = viewModel.uiState.value
        assertEquals(8, state.totalRounds)
        assertEquals(2.0f, state.ratio)
    }
}
