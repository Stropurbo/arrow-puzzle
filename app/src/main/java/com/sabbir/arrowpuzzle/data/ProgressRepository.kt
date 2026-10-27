package com.sabbir.arrowpuzzle.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore("arrow_escape")
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class PlayerPrefs(
    val unlocked: Int = 1,
    val completed: Set<Int> = emptySet(),
    val sound: Boolean = true,
    val music: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largeHitArea: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val bestMoves: Map<Int, Int> = emptyMap(),
    val bestTimes: Map<Int, Int> = emptyMap(),
    val bestStars: Map<Int, Int> = emptyMap(),
    val daily: DailyProgress = DailyProgress()
)

private fun encodeMap(map: Map<Int, Int>): Set<String> = map.map { "${it.key}:${it.value}" }.toSet()
private fun decodeMap(set: Set<String>): Map<Int, Int> = set.mapNotNull { entry ->
    val parts = entry.split(":"); if (parts.size != 2) null
    else { val k = parts[0].toIntOrNull(); val v = parts[1].toIntOrNull(); if (k == null || v == null) null else k to v }
}.toMap()

class ProgressRepository(private val context: Context) {
    private object Keys {
        val unlocked = intPreferencesKey("unlocked"); val completed = stringSetPreferencesKey("completed")
        val sound = booleanPreferencesKey("sound"); val music = booleanPreferencesKey("music"); val haptics = booleanPreferencesKey("haptics")
        val reduced = booleanPreferencesKey("reduced"); val contrast = booleanPreferencesKey("contrast"); val largeHit = booleanPreferencesKey("large_hit"); val theme = stringPreferencesKey("theme")
        val bestMoves = stringSetPreferencesKey("best_moves"); val bestTimes = stringSetPreferencesKey("best_times"); val bestStars = stringSetPreferencesKey("best_stars")
        // Daily challenge + monthly trophies.
        val dailyLastDay = longPreferencesKey("daily_last_day"); val dailyStreak = intPreferencesKey("daily_streak")
        val dailyBest = intPreferencesKey("daily_best"); val dailyMonth = stringPreferencesKey("daily_month")
        val dailyDays = stringSetPreferencesKey("daily_days"); val trophies = stringSetPreferencesKey("trophies")
        // In-progress board snapshots for process-death and background resume.
        // Story and daily boards use separate slots so starting a daily never
        // discards campaign progress.
        val session = stringPreferencesKey("game_session")
        val dailySession = stringPreferencesKey("daily_game_session")
        /** Which slot the game screen showed last, for process-death restore. */
        val activeDaily = booleanPreferencesKey("active_daily")
    }

    private fun dailyOf(p: Preferences) = DailyProgress(
        lastCompletedDay = p[Keys.dailyLastDay] ?: 0L,
        streak = p[Keys.dailyStreak] ?: 0,
        bestStreak = p[Keys.dailyBest] ?: 0,
        month = p[Keys.dailyMonth] ?: "",
        days = p[Keys.dailyDays].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
        trophies = p[Keys.trophies].orEmpty()
    )

    private fun snapshot(p: Preferences) = PlayerPrefs(
        p[Keys.unlocked] ?: 1,
        p[Keys.completed].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
        p[Keys.sound] ?: true,
        p[Keys.music] ?: true,
        p[Keys.haptics] ?: true,
        p[Keys.reduced] ?: false,
        p[Keys.contrast] ?: false,
        p[Keys.largeHit] ?: false,
        runCatching { ThemeMode.valueOf(p[Keys.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        decodeMap(p[Keys.bestMoves].orEmpty()),
        decodeMap(p[Keys.bestTimes].orEmpty()),
        decodeMap(p[Keys.bestStars].orEmpty()),
        dailyOf(p)
    )

    val prefs: Flow<PlayerPrefs> = context.dataStore.data.map { p -> snapshot(p) }

    /** Last saved story board, or null when no campaign level is in progress. */
    val session: Flow<GameSession?> =
        context.dataStore.data.map { p -> GameSession.decode(p[Keys.session]) }

    /** Last saved daily board, kept separate from campaign progress. */
    val dailySession: Flow<GameSession?> =
        context.dataStore.data.map { p -> GameSession.decode(p[Keys.dailySession]) }

    /** True when the most recently saved board was a daily puzzle. */
    val lastDaily: Flow<Boolean> = context.dataStore.data.map { it[Keys.activeDaily] ?: false }

    suspend fun complete(level: Int, moves: Int, seconds: Int, stars: Int) = context.dataStore.edit {
        it[Keys.completed] = (it[Keys.completed].orEmpty() + level.toString()); it[Keys.unlocked] = maxOf(it[Keys.unlocked] ?: 1, (level + 1).coerceAtMost(100))
        val prevMoves = decodeMap(it[Keys.bestMoves].orEmpty())[level]
        if (prevMoves == null || moves < prevMoves) it[Keys.bestMoves] = encodeMap(decodeMap(it[Keys.bestMoves].orEmpty()) + (level to moves))
        val prevTime = decodeMap(it[Keys.bestTimes].orEmpty())[level]
        if (prevTime == null || seconds < prevTime) it[Keys.bestTimes] = encodeMap(decodeMap(it[Keys.bestTimes].orEmpty()) + (level to seconds))
        val prevStars = decodeMap(it[Keys.bestStars].orEmpty())[level] ?: 0
        if (stars > prevStars) it[Keys.bestStars] = encodeMap(decodeMap(it[Keys.bestStars].orEmpty()) + (level to stars))
    }

    /** Records today's daily solve; returns the updated streak and any new trophy. */
    suspend fun completeDaily(date: LocalDate): DailyResult {
        var result = DailyResult(0, 0, 0, null)
        context.dataStore.edit { p ->
            val (updated, outcome) = DailyChallenge.record(dailyOf(p), date)
            result = outcome
            p[Keys.dailyLastDay] = updated.lastCompletedDay
            p[Keys.dailyStreak] = updated.streak
            p[Keys.dailyBest] = updated.bestStreak
            p[Keys.dailyMonth] = updated.month
            p[Keys.dailyDays] = updated.days.map { it.toString() }.toSet()
            p[Keys.trophies] = updated.trophies
        }
        return result
    }

    suspend fun update(transform: (PlayerPrefs) -> PlayerPrefs) = context.dataStore.edit { p ->
        val n = transform(snapshot(p))
        p[Keys.unlocked] = n.unlocked.coerceIn(1, 100); p[Keys.completed] = n.completed.map { it.toString() }.toSet()
        p[Keys.sound] = n.sound; p[Keys.music] = n.music; p[Keys.haptics] = n.haptics
        p[Keys.reduced] = n.reducedMotion; p[Keys.contrast] = n.highContrast; p[Keys.largeHit] = n.largeHitArea; p[Keys.theme] = n.theme.name
        p[Keys.bestMoves] = encodeMap(n.bestMoves); p[Keys.bestTimes] = encodeMap(n.bestTimes); p[Keys.bestStars] = encodeMap(n.bestStars)
        p[Keys.dailyLastDay] = n.daily.lastCompletedDay; p[Keys.dailyStreak] = n.daily.streak; p[Keys.dailyBest] = n.daily.bestStreak
        p[Keys.dailyMonth] = n.daily.month; p[Keys.dailyDays] = n.daily.days.map { it.toString() }.toSet(); p[Keys.trophies] = n.daily.trophies
    }

    suspend fun saveSession(session: GameSession) = context.dataStore.edit {
        it[Keys.session] = session.encode()
        it[Keys.activeDaily] = false
    }

    suspend fun clearSession() = context.dataStore.edit { it.remove(Keys.session) }

    suspend fun saveDailySession(session: GameSession) = context.dataStore.edit {
        it[Keys.dailySession] = session.encode()
        it[Keys.activeDaily] = true
    }

    suspend fun clearDailySession() = context.dataStore.edit { it.remove(Keys.dailySession) }

    suspend fun reset() = context.dataStore.edit { it.clear() }
}
