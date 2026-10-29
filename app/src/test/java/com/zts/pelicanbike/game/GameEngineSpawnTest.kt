package com.zts.pelicanbike.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class GameEngineSpawnTest {

    private fun midGapRandom(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextFloat(): Float = 0.5f
    }

    @Test
    fun startDoesNotInstantCrash() {
        val engine = GameEngine(Random(42))
        engine.resize(800f, 400f)
        engine.startGame()
        assertEquals(ScreenPhase.Playing, engine.phase)
        repeat(30) { engine.tick(1f / 60f) }
        assertEquals(ScreenPhase.Playing, engine.phase)
        assertEquals(0, engine.score)
    }

    @Test
    fun restartAlsoPlayable() {
        val engine = GameEngine(Random(7))
        engine.resize(800f, 400f)
        engine.startGame()
        repeat(5) { engine.tick(1f / 60f) }
        engine.restartGame()
        assertEquals(ScreenPhase.Playing, engine.phase)
        repeat(30) { engine.tick(1f / 60f) }
        assertEquals(ScreenPhase.Playing, engine.phase)
    }

    @Test
    fun canPassAtLeastOnePipeWithJumpAi() {
        val engine = GameEngine(midGapRandom())
        engine.resize(800f, 400f)
        engine.startGame()
        val dt = 1f / 60f
        var frames = 0
        val maxFrames = (20f / dt).toInt()
        var lastLog = ""
        while (frames < maxFrames && engine.phase == ScreenPhase.Playing) {
            val snap = engine.snapshot()
            val y = snap.player.y
            val vy = snap.player.vy
            val px = snap.player.x
            val nextPipe = snap.pipes
                .filter { it.x + Physics.PipeWidth >= px - 40f }
                .minByOrNull { it.x }
            val targetY = nextPipe?.gapCenterY ?: (snap.height * Physics.StartYFrac)
            // Only jump when meaningfully below the gap center (avoid ceiling)
            if (y > targetY + 12f && vy > -40f) {
                engine.jump()
            }
            engine.tick(dt)
            frames++
            if (frames % 30 == 0) {
                lastLog =
                    "t=${"%.2f".format(frames * dt)} score=${engine.score} y=${"%.1f".format(snap.player.y)} " +
                        "vy=${"%.0f".format(snap.player.vy)} target=${"%.1f".format(targetY)} " +
                        "pipeX=${nextPipe?.let { "%.0f".format(it.x) }} phase=${engine.phase}"
            }
            if (engine.score >= 2) break
        }
        if (engine.score < 1) {
            val snap = engine.snapshot()
            fail(
                "expected score>=1 got ${engine.score} phase=${engine.phase} frames=$frames " +
                    "y=${snap.player.y} ground=${snap.groundTop} last=$lastLog"
            )
        }
        assertTrue(engine.score >= 1)
    }
}
