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
    fun landAndRideWithoutGameOver() {
        val engine = GameEngine(Random(99))
        engine.resize(800f, 400f)
        engine.startGame()
        val dt = 1f / 60f
        var landed = false
        // ~4s: past invincibility, on ground, before first pipe reaches player
        repeat(240) {
            engine.tick(dt)
            val snap = engine.snapshot()
            val hitH = Physics.PlayerH * Physics.HitScale
            val bottom = snap.player.y + hitH / 2f
            if (bottom >= snap.groundTop - 0.5f) landed = true
            assertEquals(
                "must stay Playing while riding ground (frame crash from floor)",
                ScreenPhase.Playing,
                engine.phase,
            )
        }
        assertTrue("expected to have touched ground", landed)
        val snap = engine.snapshot()
        val hitH = Physics.PlayerH * Physics.HitScale
        val bottom = snap.player.y + hitH / 2f
        assertTrue(
            "feet should stay clamped to ground, bottom=$bottom ground=${snap.groundTop}",
            kotlin.math.abs(bottom - snap.groundTop) < 1.5f,
        )
    }

    @Test
    fun jumpFromGroundStillWorks() {
        val engine = GameEngine(Random(3))
        engine.resize(800f, 400f)
        engine.startGame()
        val dt = 1f / 60f
        repeat(90) { engine.tick(dt) }
        assertEquals(ScreenPhase.Playing, engine.phase)
        val before = engine.snapshot().player.y
        engine.jump()
        engine.tick(dt)
        val after = engine.snapshot().player.y
        assertTrue("jump should lift player, before=$before after=$after", after < before)
        assertEquals(ScreenPhase.Playing, engine.phase)
    }

    @Test
    fun pipeHitCausesGameOver() {
        val engine = GameEngine(midGapRandom())
        engine.resize(800f, 400f)
        engine.startGame()
        val dt = 1f / 60f
        // Stay on ground without jumping until a pipe hits
        var frames = 0
        val maxFrames = (15f / dt).toInt()
        while (frames < maxFrames && engine.phase == ScreenPhase.Playing) {
            engine.tick(dt)
            frames++
        }
        // Drain crash flash
        repeat(30) { engine.tick(dt) }
        assertEquals(
            "riding into pipe without jump should GameOver, frames=$frames score=${engine.score}",
            ScreenPhase.GameOver,
            engine.phase,
        )
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

    @Test
    fun restartAfterLandSameAsStart() {
        val engine = GameEngine(Random(11))
        engine.resize(800f, 400f)
        engine.startGame()
        val dt = 1f / 60f
        repeat(120) { engine.tick(dt) }
        engine.restartGame()
        var landed = false
        repeat(240) {
            engine.tick(dt)
            val snap = engine.snapshot()
            val hitH = Physics.PlayerH * Physics.HitScale
            if (snap.player.y + hitH / 2f >= snap.groundTop - 0.5f) landed = true
            assertEquals(ScreenPhase.Playing, engine.phase)
        }
        assertTrue(landed)
    }
}
