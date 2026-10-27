package com.sabbir.arrowpuzzle.data

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Local, offline daily-challenge state.
 *
 * One deterministic puzzle per calendar day. Completing it grows a streak and
 * fills the current month's trophy track; crossing a tier unlocks that month's
 * trophy. Everything is stored in DataStore, so it works with no network.
 */
data class DailyProgress(
    /** Epoch day of the last completed daily puzzle (0 = never). */
    val lastCompletedDay: Long = 0L,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** Month the [days] set belongs to, formatted yyyy-MM. */
    val month: String = "",
    /** Days of the current month that were completed. */
    val days: Set<Int> = emptySet(),
    /** Trophies ever earned, as "yyyy-MM:tier" entries. */
    val trophies: Set<String> = emptySet()
)

/** Outcome of finishing today's daily puzzle, used by the victory dialog. */
data class DailyResult(
    val streak: Int,
    val bestStreak: Int,
    val daysThisMonth: Int,
    val newTrophyTier: Int? = null
)

object DailyChallenge {
    /** Monthly trophy milestones, in completed days. */
    val TROPHY_TIERS = listOf(5, 10, 15, 20, 25)

    fun monthKey(date: LocalDate): String =
        "%04d-%02d".format(date.year, date.monthValue)

    fun monthLabel(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    fun shortMonth(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    fun isCompletedOn(daily: DailyProgress, date: LocalDate): Boolean =
        daily.lastCompletedDay == date.toEpochDay()

    fun daysInMonth(daily: DailyProgress, date: LocalDate): Set<Int> =
        if (daily.month == monthKey(date)) daily.days else emptySet()

    fun trophiesForMonth(daily: DailyProgress, date: LocalDate): Set<Int> =
        daily.trophies.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) null
            else if (parts[0] == monthKey(date)) parts[1].toIntOrNull() else null
        }.toSet()

    fun tiersReached(days: Int): List<Int> = TROPHY_TIERS.filter { days >= it }

    /** Human name for a trophy tier, shown under the trophy shelf. */
    fun tierName(tier: Int): String = when (tier) {
        5 -> "Bronze"
        10 -> "Silver"
        15 -> "Gold"
        20 -> "Platinum"
        25 -> "Diamond"
        else -> "$tier"
    }

    /**
     * Applies a completed daily puzzle to [daily].
     * Idempotent for the same day: replaying today never inflates the streak.
     */
    fun record(daily: DailyProgress, date: LocalDate): Pair<DailyProgress, DailyResult> {
        val today = date.toEpochDay()
        val key = monthKey(date)
        val sameMonth = daily.month == key
        val days = (if (sameMonth) daily.days else emptySet()) + date.dayOfMonth

        val streak = when {
            daily.lastCompletedDay == today -> daily.streak.coerceAtLeast(1)
            daily.lastCompletedDay == today - 1 -> daily.streak + 1
            else -> 1
        }
        val bestStreak = maxOf(daily.bestStreak, streak)

        val reached = tiersReached(days.size)
        val earned = daily.trophies
        val newTiers = reached.filter { "$key:$it" !in earned }
        val trophies = earned + newTiers.map { "$key:$it" }

        val updated = daily.copy(
            lastCompletedDay = today,
            streak = streak,
            bestStreak = bestStreak,
            month = key,
            days = days,
            trophies = trophies
        )
        return updated to DailyResult(
            streak = streak,
            bestStreak = bestStreak,
            daysThisMonth = days.size,
            newTrophyTier = newTiers.maxOrNull()
        )
    }
}
