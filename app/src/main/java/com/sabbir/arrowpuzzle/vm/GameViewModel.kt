package com.sabbir.arrowpuzzle.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sabbir.arrowpuzzle.GameConfig
import com.sabbir.arrowpuzzle.audio.GameAudioManager
import com.sabbir.arrowpuzzle.data.*
import com.sabbir.arrowpuzzle.engine.ArrowEngine
import com.sabbir.arrowpuzzle.model.GameState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameViewModel(app: Application) : AndroidViewModel(app) {
    val levels = LevelRepository(app).levels
    private val progress = ProgressRepository(app)
    val audio = GameAudioManager(app)
    val prefs = progress.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerPrefs())
    private val _game = MutableStateFlow<GameState?>(null); val game = _game.asStateFlow()

    private val _session = MutableStateFlow<GameSession?>(null); val session = _session.asStateFlow()
    private val sessionLoaded = CompletableDeferred<Unit>()
    private var sessionEpoch = 0
    private val sessionWriteLock = Mutex()
    private val storySlot = SessionSlot(progress::saveSession, progress::clearSession)
    private var restoring = false

    private var pendingTapId: String? = null
    private var appForeground = true
    private var gameVisible = false

    init {
        viewModelScope.launch {
            try {
                val rawStory = runCatching { progress.session.first() }.getOrNull()
                val storyValid = rawStory?.takeIf { !it.daily }?.takeIf { it.toGameState() != null }
                if (sessionEpoch == 0) {
                    _session.value = storyValid
                }
            } finally {
                sessionLoaded.complete(Unit)
            }
        }
    }

    init {
        viewModelScope.launch {
            prefs.collect { audio.soundEnabled = it.sound; audio.applyMusicPref(it.music) }
        }
    }

    init {
        viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                if (!appForeground || !gameVisible) continue
                _game.update { s ->
                    s?.takeIf { !it.paused && !it.completed && !it.outOfHearts }
                        ?.copy(elapsedSeconds = s.elapsedSeconds + 1) ?: s
                }
                if (++ticks % 10 == 0) persistSoon(_game.value)
            }
        }
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    /** Starts a story level immediately, discarding any previous save. */
    fun start(level: Int) {
        if (levels.isEmpty()) return
        val index = level.coerceIn(1, GameConfig.MAX_LEVELS) - 1
        begin(GameState(levels[index]), playClick = true)
    }

    /** Restores a saved board or falls back to unlocked story level. */
    fun continueGame(restoreLast: Boolean = true) {
        if (restoring || _game.value != null) return
        restoring = true
        viewModelScope.launch {
            try {
                sessionLoaded.await()
                val story = _session.value?.takeIf { it.toGameState() != null }
                val restored = story?.toGameState()
                pendingTapId = null
                if (restored != null) {
                    _game.value = restored
                    persistNow(restored)
                } else {
                    val index = prefs.value.unlocked.coerceIn(1, GameConfig.MAX_LEVELS) - 1
                    begin(GameState(levels[index]), playClick = false)
                }
            } finally {
                restoring = false
            }
        }
    }

    fun tap(id: String) {
        val before = _game.value ?: return
        if (before.movingPieceId != null) {
            pendingTapId = id
            return
        }
        val after = ArrowEngine.tap(before, id)
        _game.value = after
        val movingId = after.movingPieceId
        if (movingId != null) {
            audio.play("tap")
            audio.play("exit")
            val piece = after.level.pieces.find { it.id == movingId }
            val dur = if (prefs.value.reducedMotion) 40
                else GameConfig.exitDurationMs(piece?.cells?.size ?: 4, after.level.rows, after.level.cols)
            viewModelScope.launch {
                delay(dur.toLong() + 30L)
                _game.update { it?.let(ArrowEngine::finishMove) }
                val done = _game.value
                if (done?.completed == true) {
                    audio.play("win")
                    clearSavedSession()
                    progress.complete(
                        done.level.id,
                        done.moves,
                        done.elapsedSeconds,
                        GameConfig.starsFor(done.errors, done.hintsUsed)
                    )
                } else if (done != null) {
                    persistSoon(done)
                }
                val next = pendingTapId
                pendingTapId = null
                if (next != null && done?.completed == false && done.outOfHearts == false) tap(next)
            }
        } else if (after.invalidPieceId != null) {
            audio.play("tap")
            audio.play("invalid")
            if (after.outOfHearts) {
                audio.play("heart")
                clearSavedSession()
            } else {
                persistSoon(after)
            }
            viewModelScope.launch {
                delay(GameConfig.INVALID_ANIMATION_MS.toLong())
                _game.update { it?.let(ArrowEngine::clearInvalid) }
            }
        }
    }

    fun hint() {
        val before = _game.value ?: return
        if (before.movingPieceId != null || before.completed || before.outOfHearts) return
        val after = ArrowEngine.hint(before)
        if (after.hintsUsed == before.hintsUsed) return
        _game.value = after
        audio.play("hint")
        persistSoon(after)
    }

    fun undo() {
        val before = _game.value ?: return
        if (before.history.isEmpty() || before.movingPieceId != null || before.undoingPieceId != null || before.outOfHearts) return
        val after = ArrowEngine.undo(before)
        if (after == before) return
        _game.value = after
        audio.play("undo")
        persistSoon(after)
        val undoingId = after.undoingPieceId
        if (undoingId != null) {
            val piece = after.level.pieces.find { it.id == undoingId }
            val dur = if (prefs.value.reducedMotion) 40
                else GameConfig.exitDurationMs(piece?.cells?.size ?: 4, after.level.rows, after.level.cols)
            viewModelScope.launch {
                delay(dur.toLong())
                _game.update { it?.let(ArrowEngine::clearUndoing) }
            }
        }
    }

    fun restart() {
        pendingTapId = null
        val current = _game.value ?: return
        val fresh = levels.getOrNull(current.level.id - 1)?.let { GameState(it) } ?: return
        _game.value = fresh
        persistNow(fresh)
        audio.play("click")
    }

    fun togglePause() {
        pendingTapId = null
        val before = _game.value ?: return
        if (before.completed || before.outOfHearts) return
        val after = before.copy(paused = !before.paused)
        _game.value = after
        persistSoon(after)
        audio.play("click")
    }

    fun leaveGame() {
        pendingTapId = null
        val current = _game.value ?: return
        if (!current.completed && !current.outOfHearts && current.remainingIds.isNotEmpty()) {
            persistNow(current)
        } else {
            clearSavedSession()
        }
        _game.value = null
    }

    fun onAppBackground() {
        appForeground = false
        audio.pauseMusic()
        val current = _game.value ?: return
        if (!current.completed && !current.outOfHearts && !current.paused) {
            _game.value = current.copy(paused = true)
        }
        if (!current.completed && !current.outOfHearts && current.remainingIds.isNotEmpty()) {
            persistNow(_game.value)
        }
    }

    fun onAppForeground() {
        appForeground = true
        audio.startMusic()
    }

    fun setGameVisible(visible: Boolean) {
        gameVisible = visible
    }

    fun continueHearts() {
        _game.update {
            it?.copy(
                hearts = GameConfig.INITIAL_HEARTS,
                outOfHearts = false,
                invalidPieceId = null
            )
        }
        audio.play("click")
        persistSoon(_game.value)
    }

    fun setPrefs(change: (PlayerPrefs) -> PlayerPrefs) {
        viewModelScope.launch { progress.update(change) }
    }

    fun resetProgress() {
        pendingTapId = null
        storySlot.cancel()
        _game.value = null
        _session.value = null
        viewModelScope.launch { progress.reset() }
    }

    private fun begin(state: GameState, playClick: Boolean) {
        pendingTapId = null
        sessionEpoch++
        _game.value = state
        persistNow(state)
        if (playClick) audio.play("click")
    }

    private fun persistSoon(state: GameState?) {
        val s = state ?: return
        if (!canPersist(s)) {
            clearSavedSession()
            return
        }
        val snapshot = GameSession.from(s)
        _session.value = snapshot
        storySlot.enqueue(snapshot, SESSION_SAVE_DEBOUNCE_MS)
    }

    private fun persistNow(state: GameState?) {
        val s = state ?: return
        if (!canPersist(s)) {
            clearSavedSession()
            return
        }
        val snapshot = GameSession.from(s)
        _session.value = snapshot
        storySlot.enqueue(snapshot, 0L)
    }

    private fun clearSavedSession() {
        _session.value = null
        storySlot.enqueue(null, 0L)
    }

    private inner class SessionSlot(
        private val save: suspend (GameSession) -> Unit,
        private val clear: suspend () -> Unit
    ) {
        private var job: Job? = null
        private var seq = 0L

        fun enqueue(snapshot: GameSession?, delayMs: Long) {
            seq++
            val mine = seq
            job?.cancel()
            job = viewModelScope.launch {
                if (delayMs > 0L) delay(delayMs)
                sessionWriteLock.withLock {
                    if (mine != seq) return@withLock
                    runCatching {
                        if (snapshot == null) clear() else save(snapshot)
                    }
                }
            }
        }

        fun cancel() {
            seq++
            job?.cancel()
            job = null
        }
    }

    private fun canPersist(state: GameState): Boolean =
        !state.completed && !state.outOfHearts && state.remainingIds.isNotEmpty()

    private companion object {
        const val SESSION_SAVE_DEBOUNCE_MS = 320L
    }
}
