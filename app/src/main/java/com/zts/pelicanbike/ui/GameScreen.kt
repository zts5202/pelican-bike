package com.zts.pelicanbike.ui

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zts.pelicanbike.game.ScreenPhase

@Composable
fun GameScreen(vm: GameViewModel) {
    val world by vm.world.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastScore by remember { mutableStateOf(0) }
    var scorePulse by remember { mutableStateOf(false) }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val wDp = canvasSize.width / density.density
            val hDp = canvasSize.height / density.density
            vm.onSizeChanged(wDp, hDp)
        }
    }

    // Frame loop — FlappyBirdClone style withFrameNanos
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000.0).toFloat()
                    vm.tick(dt)
                }
                last = now
            }
        }
    }

    LaunchedEffect(world.score) {
        if (world.score > lastScore && world.phase == ScreenPhase.Playing) {
            scorePulse = true
            kotlinx.coroutines.delay(280)
            scorePulse = false
        }
        lastScore = world.score
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(world.phase) {
                detectTapGestures {
                    if (world.phase == ScreenPhase.Playing) {
                        vm.jump()
                    }
                }
            },
    ) {
        GameWorldCanvas(
            world = world,
            reduceMotion = reduceMotion,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Crossfade(
                targetState = world.phase,
                animationSpec = tween(220),
                label = "phase",
            ) { phase ->
                when (phase) {
                    ScreenPhase.Start -> StartOverlay(
                        world = world,
                        onStart = { vm.start() },
                        onToggleMute = { vm.toggleMute() },
                    )
                    ScreenPhase.Playing -> PlayingHud(
                        world = world,
                        scorePulse = scorePulse,
                    )
                    ScreenPhase.GameOver -> GameOverOverlay(
                        world = world,
                        onRetry = { vm.restart() },
                        onTitle = { vm.toTitle() },
                    )
                }
            }
        }
    }
}
