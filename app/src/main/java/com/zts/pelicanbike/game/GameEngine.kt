package com.zts.pelicanbike.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Frame-driven side-scroller engine (Flappy-style pipes).
 * Call [tick] each frame with dt in seconds; all units are dp.
 */
class GameEngine(
    private val random: Random = Random.Default,
) {
    var phase: ScreenPhase = ScreenPhase.Start
        private set
    var score: Int = 0
        private set
    var highScore: Int = 0
        private set
    var isNewRecord: Boolean = false
        private set
    var muted: Boolean = false

    private var width = 800f
    private var height = 400f
    private var player = Player(x = 0f, y = 0f, vy = 0f)
    private val pipes = mutableListOf<PipePair>()
    private var scrollSpeed = Physics.Scroll0
    private var groundOffset = 0f
    private var parallax0 = 0f
    private var parallax1 = 0f
    private var flashAlpha = 0f
    private var shakeX = 0f
    private var flashTimer = 0f
    private var pendingGameOver = false
    private var spawnCursor = 0f
    /** Remaining invincible time (s) after start/restart; skips ground/pipe collision. */
    private var invincibleLeft = 0f

    val groundTop: Float get() = height * (1f - Physics.GroundFrac)

    fun setHighScore(value: Int) {
        highScore = max(0, value)
    }

    fun resize(w: Float, h: Float) {
        if (w <= 1f || h <= 1f) return
        width = w
        height = h
        if (phase == ScreenPhase.Start && pipes.isEmpty()) {
            resetWorld(keepScore = true)
        } else {
            player = player.copy(x = width * Physics.PlayerXFrac)
        }
    }

    fun startGame() {
        resetWorld(keepScore = false)
        phase = ScreenPhase.Playing
        invincibleLeft = Physics.StartInvincible
        isNewRecord = false
    }

    fun restartGame() {
        resetWorld(keepScore = false)
        phase = ScreenPhase.Playing
        invincibleLeft = Physics.StartInvincible
        isNewRecord = false
    }

    fun backToTitle() {
        resetWorld(keepScore = true)
        phase = ScreenPhase.Start
        invincibleLeft = 0f
        isNewRecord = false
    }

    fun jump() {
        if (phase != ScreenPhase.Playing || pendingGameOver) return
        player = player.copy(vy = Physics.JumpVelocity)
    }

    fun toggleMute() {
        muted = !muted
    }

    private fun resetWorld(keepScore: Boolean) {
        score = if (keepScore) score else 0
        scrollSpeed = Physics.Scroll0
        groundOffset = 0f
        parallax0 = 0f
        parallax1 = 0f
        flashAlpha = 0f
        shakeX = 0f
        flashTimer = 0f
        pendingGameOver = false
        invincibleLeft = 0f
        pipes.clear()
        val px = width * Physics.PlayerXFrac
        // Mid-air spawn (was ground-hugging → instant crash on first frames)
        val py = height * Physics.StartYFrac
        player = Player(
            x = px,
            y = py,
            vy = Physics.JumpVelocity * Physics.StartVyFrac,
            wheelAngle = 0f,
            bobPhase = 0f,
        )
        spawnCursor = width + Physics.FirstPipeOffset
        // seed a few pipes ahead
        repeat(4) { spawnPipe() }
    }

    private fun gapHeightForScore(): Float {
        val steps = score / 9
        return max(Physics.GapMin, Physics.GapStart - steps * 8f)
    }

    private fun spacingForScore(): Float {
        val steps = score / 8
        return max(Physics.PipeSpacingMin, Physics.PipeSpacingStart - steps * 10f)
    }

    private fun spawnPipe() {
        val gap = gapHeightForScore()
        val minCenter = gap / 2f + 48f
        val maxCenter = groundTop - gap / 2f - 48f
        val center = if (maxCenter <= minCenter) {
            groundTop / 2f
        } else {
            minCenter + random.nextFloat() * (maxCenter - minCenter)
        }
        pipes += PipePair(x = spawnCursor, gapCenterY = center, gapHeight = gap)
        spawnCursor += spacingForScore()
    }

    fun tick(dt: Float): WorldSnapshot {
        val clampedDt = min(dt, 0.05f)
        when (phase) {
            ScreenPhase.Start -> {
                // slow preview scroll
                val preview = 24f
                groundOffset = (groundOffset + preview * clampedDt) % 60f
                parallax0 = (parallax0 + preview * 0.2f * clampedDt) % (width + 200f)
                parallax1 = (parallax1 + preview * 0.5f * clampedDt) % (width + 200f)
                player = player.copy(
                    bobPhase = player.bobPhase + clampedDt * (Math.PI.toFloat() * 2f / 0.28f),
                    wheelAngle = player.wheelAngle + (preview / 14f) * clampedDt,
                )
            }
            ScreenPhase.Playing -> {
                if (pendingGameOver) {
                    flashTimer -= clampedDt
                    flashAlpha = max(0f, flashTimer / 0.12f) * 0.35f
                    shakeX = if (flashTimer > 0.04f) {
                        if (((flashTimer * 40).toInt() % 2) == 0) 3f else -3f
                    } else 0f
                    if (flashTimer <= 0f) {
                        flashAlpha = 0f
                        shakeX = 0f
                        pendingGameOver = false
                        phase = ScreenPhase.GameOver
                        if (score > highScore) {
                            isNewRecord = true
                            highScore = score
                        }
                    }
                } else {
                    stepPlaying(clampedDt)
                }
            }
            ScreenPhase.GameOver -> {
                // freeze world
            }
        }
        return snapshot()
    }

    private fun stepPlaying(dt: Float) {
        if (invincibleLeft > 0f) {
            invincibleLeft = max(0f, invincibleLeft - dt)
        }
        scrollSpeed = min(
            Physics.ScrollMax,
            Physics.Scroll0 + (score / 5) * Physics.ScrollGainEvery5,
        )
        // player physics
        var vy = player.vy + Physics.Gravity * dt
        vy = min(vy, Physics.MaxFall)
        var y = player.y + vy * dt
        val bob = player.bobPhase + dt * (Math.PI.toFloat() * 2f / 0.28f)
        val wheelR = 14f
        val wheelAng = player.wheelAngle + (scrollSpeed / wheelR) * dt
        player = player.copy(
            y = y,
            vy = vy,
            bobPhase = bob,
            wheelAngle = wheelAng,
            x = width * Physics.PlayerXFrac,
        )
        groundOffset = (groundOffset + scrollSpeed * dt) % 60f
        parallax0 = (parallax0 + scrollSpeed * 0.2f * dt) % (width + 200f)
        parallax1 = (parallax1 + scrollSpeed * 0.5f * dt) % (width + 200f)

        // move pipes
        for (i in pipes.indices) {
            pipes[i] = pipes[i].copy(x = pipes[i].x - scrollSpeed * dt)
        }
        pipes.removeAll { it.x + Physics.PipeWidth < -40f }
        val rightMost = pipes.maxOfOrNull { it.x } ?: width
        while (rightMost < width + 400f && (pipes.maxOfOrNull { it.x } ?: 0f) < width + spacingForScore()) {
            spawnCursor = (pipes.maxOfOrNull { it.x } ?: width) + spacingForScore()
            spawnPipe()
            if (pipes.size > 12) break
        }
        if ((pipes.maxOfOrNull { it.x } ?: 0f) < width + spacingForScore()) {
            spawnCursor = (pipes.maxOfOrNull { it.x } ?: width) + spacingForScore()
            spawnPipe()
        }

        // scoring
        val hitW = Physics.PlayerW * Physics.HitScale
        val hitH = Physics.PlayerH * Physics.HitScale
        val px = player.x
        val py = player.y
        val left = px - hitW / 2f
        val right = px + hitW / 2f
        val top = py - hitH / 2f
        val bottom = py + hitH / 2f

        for (i in pipes.indices) {
            val p = pipes[i]
            if (!p.scored && px > p.x + Physics.PipeWidth) {
                pipes[i] = p.copy(scored = true)
                score += 1
            }
        }

        // During start invincibility: skip lethal hits; bounce off floor/ceiling
        // so the player is still airborne when invincibility ends.
        if (invincibleLeft > 0f) {
            var cy = player.y
            var cvy = player.vy
            if (bottom >= groundTop) {
                cy = groundTop - hitH / 2f - 1f
                cvy = Physics.JumpVelocity * 0.55f
            }
            if (top <= 0f) {
                cy = hitH / 2f + 1f
                cvy = max(cvy, -Physics.JumpVelocity * 0.25f)
            }
            player = player.copy(y = cy, vy = cvy)
            return
        }

        // collisions: ground / ceiling
        if (bottom >= groundTop || top <= 0f) {
            triggerCrash()
            return
        }
        // pipes AABB
        for (p in pipes) {
            val gapTop = p.gapCenterY - p.gapHeight / 2f
            val gapBot = p.gapCenterY + p.gapHeight / 2f
            val pipeL = p.x
            val pipeR = p.x + Physics.PipeWidth
            if (right > pipeL && left < pipeR) {
                if (top < gapTop || bottom > gapBot) {
                    triggerCrash()
                    return
                }
            }
        }
    }

    private fun triggerCrash() {
        pendingGameOver = true
        flashTimer = 0.12f
        flashAlpha = 0.35f
        shakeX = 3f
    }

    fun snapshot(): WorldSnapshot = WorldSnapshot(
        phase = phase,
        player = player,
        pipes = pipes.toList(),
        score = score,
        highScore = highScore,
        isNewRecord = isNewRecord,
        scrollSpeed = scrollSpeed,
        groundOffset = groundOffset,
        parallax0 = parallax0,
        parallax1 = parallax1,
        flashAlpha = flashAlpha,
        shakeX = shakeX,
        width = width,
        height = height,
        groundTop = groundTop,
        muted = muted,
    )
}
