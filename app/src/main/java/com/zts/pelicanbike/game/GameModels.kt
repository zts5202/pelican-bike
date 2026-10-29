package com.zts.pelicanbike.game

/** Y axis: down is positive (Android canvas). Units: density-independent pixels (dp). */
enum class ScreenPhase {
    Start,
    Playing,
    GameOver,
}

data class PipePair(
    val x: Float,
    val gapCenterY: Float,
    val gapHeight: Float,
    val scored: Boolean = false,
)

data class Player(
    val x: Float,
    val y: Float,
    val vy: Float,
    val wheelAngle: Float = 0f,
    val bobPhase: Float = 0f,
)

data class WorldSnapshot(
    val phase: ScreenPhase,
    val player: Player,
    val pipes: List<PipePair>,
    val score: Int,
    val highScore: Int,
    val isNewRecord: Boolean,
    val scrollSpeed: Float,
    val groundOffset: Float,
    val parallax0: Float,
    val parallax1: Float,
    val flashAlpha: Float,
    val shakeX: Float,
    val width: Float,
    val height: Float,
    val groundTop: Float,
    val muted: Boolean,
)

object Physics {
    const val JumpVelocity = -480f // dp/s
    const val Gravity = 1600f // dp/s^2
    const val MaxFall = 800f // dp/s
    const val Scroll0 = 140f // dp/s
    const val ScrollGainEvery5 = 10f
    const val ScrollMax = 280f
    const val PipeWidth = 52f
    const val GapStart = 180f
    const val GapMin = 120f
    const val PipeSpacingStart = 240f
    const val PipeSpacingMin = 160f
    const val PlayerW = 80f
    const val PlayerH = 60f
    const val HitScale = 0.85f
    const val GroundFrac = 0.20f
    const val PlayerXFrac = 0.28f
    /** Spawn Y as fraction of canvas height (mid-air; was ground-hugging). */
    const val StartYFrac = 0.45f
    /** First pipe offset past right edge (dp). */
    const val FirstPipeOffset = 220f
    /** Invincible duration after start/restart (seconds). */
    const val StartInvincible = 0.7f
    /** Opening upward boost as fraction of [JumpVelocity]. */
    const val StartVyFrac = 0.35f
}
