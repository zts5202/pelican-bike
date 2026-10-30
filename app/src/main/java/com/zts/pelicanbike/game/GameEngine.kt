package com.zts.pelicanbike.game

import kotlin.math.abs
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
    private var parallax2 = 0f
    private var flashAlpha = 0f
    private var shakeX = 0f
    private var flashTimer = 0f
    private var pendingGameOver = false
    private var spawnCursor = 0f
    /** Remaining invincible time (s) after start/restart; skips pipe/ceiling collision (ground always clamps). */
    private var invincibleLeft = 0f
    private var wasAirborne = true
    private val dust = mutableListOf<DustParticle>()
    private var plusOne: PlusOneFx? = null

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
        wasAirborne = true
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
        parallax2 = 0f
        flashAlpha = 0f
        shakeX = 0f
        flashTimer = 0f
        pendingGameOver = false
        invincibleLeft = 0f
        wasAirborne = true
        dust.clear()
        plusOne = null
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

    private fun randomSkin(): ObstacleSkin {
        return when (random.nextInt(3)) {
            0 -> ObstacleSkin.Barricade
            1 -> ObstacleSkin.Arch
            else -> ObstacleSkin.Stump
        }
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
        pipes += PipePair(
            x = spawnCursor,
            gapCenterY = center,
            gapHeight = gap,
            skin = randomSkin(),
        )
        spawnCursor += spacingForScore()
    }

    fun tick(dt: Float): WorldSnapshot {
        val clampedDt = min(dt, 0.05f)
        when (phase) {
            ScreenPhase.Start -> {
                // slow preview scroll
                val preview = 24f
                groundOffset = (groundOffset + preview * clampedDt) % 60f
                parallax0 = (parallax0 + preview * 0.15f * clampedDt) % (width + 200f)
                parallax1 = (parallax1 + preview * 0.25f * clampedDt) % (width + 200f)
                parallax2 = (parallax2 + preview * 0.45f * clampedDt) % (width + 200f)
                player = player.copy(
                    bobPhase = player.bobPhase + clampedDt * (Math.PI.toFloat() * 2f / 0.28f),
                    wheelAngle = player.wheelAngle + (preview / Physics.RearWheelR) * clampedDt,
                )
                tickFx(clampedDt)
            }
            ScreenPhase.Playing -> {
                if (pendingGameOver) {
                    flashTimer -= clampedDt
                    flashAlpha = max(0f, flashTimer / 0.12f) * 0.35f
                    shakeX = if (flashTimer > 0.04f) {
                        if (((flashTimer * 40).toInt() % 2) == 0) 3f else -3f
                    } else 0f
                    tickFx(clampedDt)
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
                // freeze world; let +1/dust finish fading
                tickFx(clampedDt)
            }
        }
        return snapshot()
    }

    private fun tickFx(dt: Float) {
        if (dust.isNotEmpty()) {
            val next = ArrayList<DustParticle>(dust.size)
            for (d in dust) {
                val age = d.age + dt
                if (age < d.life) {
                    next += d.copy(
                        x = d.x + d.vx * dt,
                        y = d.y + d.vy * dt,
                        age = age,
                        vy = d.vy + 40f * dt,
                    )
                }
            }
            dust.clear()
            dust.addAll(next)
        }
        plusOne?.let { p ->
            val age = p.age + dt
            plusOne = if (age >= p.life) null else p.copy(age = age)
        }
    }

    private fun spawnDust() {
        dust.clear()
        val hitch = Physics.PlayerH * Physics.HitScale
        val footY = player.y + hitch / 2f
        val baseX = player.x - 18f // near rear wheel in bbox
        val offsets = floatArrayOf(-14f, -6f, 6f, 14f)
        for (ox in offsets) {
            dust += DustParticle(
                x = baseX + ox,
                y = footY,
                vx = ox * 1.2f,
                vy = -30f - abs(ox) * 0.4f,
                age = 0f,
            )
        }
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
        val wheelAng = player.wheelAngle + (scrollSpeed / Physics.RearWheelR) * dt
        player = player.copy(
            y = y,
            vy = vy,
            bobPhase = bob,
            wheelAngle = wheelAng,
            x = width * Physics.PlayerXFrac,
        )
        groundOffset = (groundOffset + scrollSpeed * dt) % 60f
        parallax0 = (parallax0 + scrollSpeed * 0.15f * dt) % (width + 200f)
        parallax1 = (parallax1 + scrollSpeed * 0.25f * dt) % (width + 200f)
        parallax2 = (parallax2 + scrollSpeed * 0.45f * dt) % (width + 200f)

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
                plusOne = PlusOneFx(x = px + 20f, y = py - hitH / 2f - 8f, age = 0f)
            }
        }

        // Landing dust: was airborne, now near ground with vy>=0
        val nearGround = bottom >= groundTop - 3f && bottom <= groundTop + 6f
        if (wasAirborne && nearGround && player.vy >= 0f) {
            spawnDust()
            wasAirborne = false
        }
        if (bottom < groundTop - 8f || player.vy < -40f) {
            wasAirborne = true
        }

        tickFx(dt)

        // Ground never kills: clamp feet to groundTop and clear downward velocity.
        // Jump still works (sets negative vy). Invincibility only skips pipe/ceiling hits.
        var cy = player.y
        var cvy = player.vy
        val bottomNow = cy + hitH / 2f
        if (bottomNow >= groundTop) {
            cy = groundTop - hitH / 2f
            cvy = min(cvy, 0f)
            if (wasAirborne) {
                spawnDust()
                wasAirborne = false
            }
            player = player.copy(y = cy, vy = cvy)
        }

        if (invincibleLeft > 0f) {
            // Soft ceiling clamp only while invincible (no crash)
            val topNow = player.y - hitH / 2f
            if (topNow <= 0f) {
                player = player.copy(
                    y = hitH / 2f + 1f,
                    vy = max(player.vy, 0f),
                )
            }
            return
        }

        // Lethal: ceiling only (not ground)
        val topLethal = player.y - hitH / 2f
        if (topLethal <= 0f) {
            triggerCrash()
            return
        }
        // pipes AABB
        val left2 = player.x - hitW / 2f
        val right2 = player.x + hitW / 2f
        val top2 = player.y - hitH / 2f
        val bottom2 = player.y + hitH / 2f
        for (p in pipes) {
            val gapTop = p.gapCenterY - p.gapHeight / 2f
            val gapBot = p.gapCenterY + p.gapHeight / 2f
            val pipeL = p.x
            val pipeR = p.x + Physics.PipeWidth
            if (right2 > pipeL && left2 < pipeR) {
                if (top2 < gapTop || bottom2 > gapBot) {
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
        parallax2 = parallax2,
        dust = dust.toList(),
        plusOne = plusOne,
        flashAlpha = flashAlpha,
        shakeX = shakeX,
        width = width,
        height = height,
        groundTop = groundTop,
        muted = muted,
    )
}
