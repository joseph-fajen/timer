package com.igygtimer.repository

import com.igygtimer.model.TimerPhase
import com.igygtimer.model.WorkoutConfig
import com.igygtimer.util.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerRepositoryTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var repository: TimerRepository

    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider(currentTimeMs = 1000L)
        repository = TimerRepository(timeProvider = fakeTimeProvider)
    }

    @Test
    fun `initial state is Idle`() {
        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Idle)
    }

    @Test
    fun `startWorkout transitions to Work phase`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.5f, totalRounds = 5))

        val state = repository.uiState.value
        assertTrue("Expected Work phase but got ${state.phase}", state.phase is TimerPhase.Work)
        assertEquals(1, state.currentRound)
        assertEquals(5, state.totalRounds)
        assertEquals(1.5f, state.ratio)
    }

    @Test
    fun `onWorkDone transitions to Rest with correct duration`() {
        repository.startWorkout(WorkoutConfig(ratio = 2.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(10_000)
        repository.onWorkDone()

        val state = repository.uiState.value
        assertTrue("Expected Rest phase but got ${state.phase}", state.phase is TimerPhase.Rest)

        val rest = state.phase as TimerPhase.Rest
        assertEquals(20_000L, rest.durationMs)
    }

    @Test
    fun `pause from Work preserves elapsed time`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(5_000)
        repository.tick()
        repository.pause()

        val state = repository.uiState.value
        assertTrue("Expected Paused phase but got ${state.phase}", state.phase is TimerPhase.Paused)

        val paused = state.phase as TimerPhase.Paused
        assertTrue(paused.from is TimerPhase.Work)
        assertEquals(5_000L, paused.remainingMs)
    }

    @Test
    fun `stop resets to Idle`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        repository.stop()

        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Idle)
    }

    @Test
    fun `tick updates displayTimeMs during Work`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(2_500)
        repository.tick()

        val state = repository.uiState.value
        assertEquals(2_500L, state.displayTimeMs)
    }

    @Test
    fun `tick counts down during Rest`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(10_000)
        repository.onWorkDone()

        fakeTimeProvider.advanceBy(3_000)
        repository.tick()

        val state = repository.uiState.value
        assertEquals(7_000L, state.displayTimeMs)
    }

    @Test
    fun `rest complete advances to next round`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(5_000)
        repository.onWorkDone()

        fakeTimeProvider.advanceBy(5_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue("Expected Work phase but got ${state.phase}", state.phase is TimerPhase.Work)
        assertEquals(2, state.currentRound)
    }

    @Test
    fun `final rest complete transitions to Complete`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 1))

        fakeTimeProvider.advanceBy(5_000)
        repository.onWorkDone()

        fakeTimeProvider.advanceBy(5_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue("Expected Complete phase but got ${state.phase}", state.phase is TimerPhase.Complete)
    }

    @Test
    fun `isActive returns true during workout`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))
        assertTrue(repository.isActive())
    }

    @Test
    fun `isActive returns false when Idle`() {
        assertTrue(!repository.isActive())
    }

    @Test
    fun `resume from Work continues counting`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(5_000)
        repository.tick()
        repository.pause()

        fakeTimeProvider.advanceBy(1_000)
        repository.resume()

        fakeTimeProvider.advanceBy(2_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Work)
        assertEquals(7_000L, state.displayTimeMs)
    }

    @Test
    fun `resume from Rest continues countdown`() {
        repository.startWorkout(WorkoutConfig(ratio = 1.0f, totalRounds = 3))

        fakeTimeProvider.advanceBy(10_000)
        repository.onWorkDone()

        fakeTimeProvider.advanceBy(3_000)
        repository.tick()
        repository.pause()

        val pausedState = repository.uiState.value
        val paused = pausedState.phase as TimerPhase.Paused
        assertEquals(7_000L, paused.remainingMs)

        fakeTimeProvider.advanceBy(1_000)
        repository.resume()

        fakeTimeProvider.advanceBy(2_000)
        repository.tick()

        val state = repository.uiState.value
        assertTrue(state.phase is TimerPhase.Rest)
        assertEquals(5_000L, state.displayTimeMs)
    }
}
