package com.zts.pelicanbike.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zts.pelicanbike.data.HighScoreStore
import com.zts.pelicanbike.game.GameEngine
import com.zts.pelicanbike.game.ScreenPhase
import com.zts.pelicanbike.game.WorldSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HighScoreStore(app)
    private val engine = GameEngine()

    private val _world = MutableStateFlow(engine.snapshot())
    val world: StateFlow<WorldSnapshot> = _world.asStateFlow()

    private var lastPersistedHigh = 0

    init {
        viewModelScope.launch {
            val hs = store.highScore.first()
            engine.setHighScore(hs)
            lastPersistedHigh = hs
            _world.value = engine.snapshot()
        }
        viewModelScope.launch {
            store.highScore.collect { hs ->
                if (hs > engine.highScore) {
                    engine.setHighScore(hs)
                    lastPersistedHigh = hs
                    _world.value = engine.snapshot()
                }
            }
        }
    }

    fun onSizeChanged(wDp: Float, hDp: Float) {
        engine.resize(wDp, hDp)
        _world.value = engine.snapshot()
    }

    fun tick(dt: Float) {
        val prevPhase = engine.phase
        val snap = engine.tick(dt)
        _world.value = snap
        if (prevPhase == ScreenPhase.Playing && snap.phase == ScreenPhase.GameOver) {
            viewModelScope.launch {
                val next = store.updateIfHigher(snap.score)
                lastPersistedHigh = next
                engine.setHighScore(next)
                _world.value = engine.snapshot()
            }
        }
    }

    fun start() {
        engine.startGame()
        _world.value = engine.snapshot()
    }

    fun restart() {
        engine.restartGame()
        _world.value = engine.snapshot()
    }

    fun toTitle() {
        engine.backToTitle()
        _world.value = engine.snapshot()
    }

    fun jump() {
        engine.jump()
        _world.value = engine.snapshot()
    }

    fun toggleMute() {
        engine.toggleMute()
        _world.value = engine.snapshot()
    }
}
