package com.sabbir.arrowpuzzle

import com.sabbir.arrowpuzzle.data.DailyChallenge
import com.sabbir.arrowpuzzle.data.DailyProgress
import com.sabbir.arrowpuzzle.engine.DailyLevelFactory
import com.sabbir.arrowpuzzle.engine.LevelSolver
import com.sabbir.arrowpuzzle.engine.LevelValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyChallengeTest {

    private val day1 = LocalDate.of(2026, 10, 1)

    @Test
    fun firstCompletionStartsOneDayStreak() {
        val (daily, result) = DailyChallenge.record(DailyProgress(), day1)
        assertEquals(1, result.streak)
        assertEquals(1, daily.days.size)
        assertTrue(DailyChallenge.isCompletedOn(daily, day1))
    }

    @Test
    fun consecutiveDaysGrowTheStreak() {
        var daily = DailyProgress()
        for (i in 0 until 5) {
            val (next, result) = DailyChallenge.record(daily, day1.plusDays(i.toLong()))
            daily = next
            assertEquals(i + 1, result.streak)
        }
        assertEquals(5, daily.bestStreak)
        assertEquals(5, daily.days.size)
    }

    @Test
    fun sameDayReplayDoesNotInflateTheStreak() {
        val (once, _) = DailyChallenge.record(DailyProgress(), day1)
        val (twice, result) = DailyChallenge.record(once, day1)
        assertEquals(1, result.streak)
        assertEquals(1, twice.days.size)
        assertEquals(once.trophies, twice.trophies)
    }

    @Test
    fun skippedDayResetsTheStreak() {
        val (first, _) = DailyChallenge.record(DailyProgress(), day1)
        val (second, result) = DailyChallenge.record(first, day1.plusDays(3))
        assertEquals(1, result.streak)
        assertEquals(1, first.bestStreak)
        assertEquals(2, second.days.size)
    }

    @Test
    fun fifthDayUnlocksTheMonthlyTrophy() {
        var daily = DailyProgress()
        var lastNewTier: Int? = null
        for (i in 0 until 5) {
            val (next, result) = DailyChallenge.record(daily, day1.plusDays(i.toLong()))
            daily = next
            if (result.newTrophyTier != null) lastNewTier = result.newTrophyTier
        }
        assertEquals(5, lastNewTier)
        assertTrue(5 in DailyChallenge.trophiesForMonth(daily, day1))
        assertEquals(1, daily.trophies.size)
    }

    @Test
    fun newMonthStartsAFreshTrophyTrack() {
        var daily = DailyProgress()
        for (i in 0 until 6) {
            daily = DailyChallenge.record(daily, day1.plusDays(i.toLong())).first
        }
        val november = LocalDate.of(2026, 11, 1)
        val (next, result) = DailyChallenge.record(daily, november)
        assertEquals(1, result.streak)
        assertEquals(1, result.daysThisMonth)
        assertEquals(0, DailyChallenge.trophiesForMonth(next, november).size)
        // October's trophy is still in the case.
        assertEquals(1, DailyChallenge.trophiesForMonth(next, day1).size)
    }

    @Test
    fun generatedDailyLevelsAreValidAndSolvable() {
        val dates = listOf(
            LocalDate.of(2026, 10, 27),
            LocalDate.of(2026, 10, 31),
            LocalDate.of(2026, 12, 25),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(2027, 1, 1)
        )
        for (date in dates) {
            val level = DailyLevelFactory.generate(date)
            val errors = LevelValidator.errors(level)
            assertTrue("$date -> $errors", errors.isEmpty())
            assertTrue("$date pieces=${level.pieces.size}", level.pieces.size >= 12)
            assertEquals(level.pieces.size, LevelSolver.solve(level)?.size)
            // Deterministic for a given date.
            assertEquals(level.pieces.size, DailyLevelFactory.generate(date).pieces.size)
        }
    }
}
