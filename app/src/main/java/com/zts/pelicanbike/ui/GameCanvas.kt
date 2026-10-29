package com.zts.pelicanbike.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zts.pelicanbike.game.Physics
import com.zts.pelicanbike.game.WorldSnapshot
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameWorldCanvas(
    world: WorldSnapshot,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    Canvas(modifier = modifier) {
        // Convert dp-based world to px: engine stores values as "dp numbers"
        // We treat canvas size in px and scale: 1 engine unit = 1.dp in px
        val s = density
        drawSky()
        drawParallax(world, s, reduceMotion)
        drawGround(world, s)
        drawPipes(world, s)
        drawPlayer(world, s, reduceMotion)
        if (world.flashAlpha > 0f) {
            drawRect(PbColors.dangerFlash.copy(alpha = world.flashAlpha))
        }
    }
}

private fun DrawScope.drawSky() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(PbColors.bgSkyTop, PbColors.bgSkyBottom),
        ),
    )
}

private fun DrawScope.drawParallax(world: WorldSnapshot, s: Float, reduceMotion: Boolean) {
    val speedFactor = if (reduceMotion) 0f else 1f
    val off0 = world.parallax0 * s * speedFactor
    val off1 = world.parallax1 * s * speedFactor
    val groundY = world.groundTop * s
    // L0 hills
    for (i in -1..3) {
        val cx = i * size.width * 0.55f - (off0 % (size.width * 0.55f))
        drawOval(
            color = PbColors.parallax.copy(alpha = 0.9f),
            topLeft = Offset(cx, groundY - 90.dp.toPx()),
            size = Size(size.width * 0.5f, 120.dp.toPx()),
        )
    }
    // L1 smaller
    for (i in -1..4) {
        val cx = i * size.width * 0.4f - (off1 % (size.width * 0.4f))
        drawOval(
            color = PbColors.parallax.copy(alpha = 0.7f),
            topLeft = Offset(cx, groundY - 55.dp.toPx()),
            size = Size(size.width * 0.32f, 70.dp.toPx()),
        )
    }
}

private fun DrawScope.drawGround(world: WorldSnapshot, s: Float) {
    val groundY = world.groundTop * s
    drawRect(
        color = PbColors.ground,
        topLeft = Offset(0f, groundY),
        size = Size(size.width, size.height - groundY),
    )
    drawRect(
        color = PbColors.groundLine,
        topLeft = Offset(0f, groundY),
        size = Size(size.width, 2.dp.toPx()),
    )
    // dashed road lines
    val dashW = 24.dp.toPx()
    val gap = 36.dp.toPx()
    val period = dashW + gap
    val start = -((world.groundOffset * s) % period)
    var x = start
    val lineY = groundY + (size.height - groundY) * 0.45f
    while (x < size.width) {
        drawRoundRect(
            color = PbColors.groundLine,
            topLeft = Offset(x, lineY),
            size = Size(dashW, 3.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        x += period
    }
}

private fun DrawScope.drawPipes(world: WorldSnapshot, s: Float) {
    val pipeW = Physics.PipeWidth * s
    val r = 8.dp.toPx()
    val accent = 2.dp.toPx()
    for (p in world.pipes) {
        val x = p.x * s
        val gapTop = (p.gapCenterY - p.gapHeight / 2f) * s
        val gapBot = (p.gapCenterY + p.gapHeight / 2f) * s
        // top pipe
        if (gapTop > 0f) {
            drawRoundRect(
                color = PbColors.obstacle,
                topLeft = Offset(x, 0f),
                size = Size(pipeW, gapTop),
                cornerRadius = CornerRadius(r),
            )
            drawRect(
                color = PbColors.obstacleAccent,
                topLeft = Offset(x, gapTop - accent),
                size = Size(pipeW, accent),
            )
        }
        // bottom pipe
        val groundY = world.groundTop * s
        val botH = groundY - gapBot
        if (botH > 0f) {
            drawRoundRect(
                color = PbColors.obstacle,
                topLeft = Offset(x, gapBot),
                size = Size(pipeW, botH),
                cornerRadius = CornerRadius(r),
            )
            drawRect(
                color = PbColors.obstacleAccent,
                topLeft = Offset(x, gapBot),
                size = Size(pipeW, accent),
            )
        }
    }
}

private fun DrawScope.drawPlayer(world: WorldSnapshot, s: Float, reduceMotion: Boolean) {
    val p = world.player
    val cx = p.x * s + world.shakeX * s
    val bobAmp = if (reduceMotion) 0f else {
        val flying = kotlin.math.abs(p.vy) > 40f
        if (flying) 0.6f else 2.5f
    }
    val bob = sin(p.bobPhase.toDouble()).toFloat() * bobAmp * s
    val cy = p.y * s + bob

    withTransform({
        translate(cx, cy)
    }) {
        val rw = Offset(-22f * s, 10f * s)
        val fw = Offset(20f * s, 10f * s)
        val rearR = 14f * s
        val frontR = 13.5f * s

        // z1 rear wheel → z2 frame → z3 front wheel → z4/5 pelican
        drawWheel(rw, rearR, p.wheelAngle)
        drawFrame(rw, fw, s)
        drawWheel(fw, frontR, p.wheelAngle * 1.02f)
        val wingTilt = when {
            p.vy < -50f -> -8f
            p.vy > 80f -> 4f
            else -> 0f
        }
        drawPelican(s, wingTilt)
    }
}

private fun DrawScope.drawWheel(center: Offset, radius: Float, angle: Float) {
    drawCircle(
        color = PbColors.wheelTire,
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.28f),
    )
    drawCircle(color = PbColors.bikeFrameDark, radius = radius * 0.22f, center = center)
    val spokes = 4
    for (i in 0 until spokes) {
        val a = angle + i * (Math.PI.toFloat() / spokes)
        val dx = cos(a) * radius * 0.85f
        val dy = sin(a) * radius * 0.85f
        drawLine(
            color = PbColors.wheelSpoke,
            start = center,
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = maxOf(1.5f, radius * 0.08f),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawFrame(rear: Offset, front: Offset, s: Float) {
    val seat = Offset((rear.x + front.x) / 2f - 4f * s, rear.y - 22f * s)
    val pedal = Offset((rear.x + front.x) / 2f, rear.y - 2f * s)
    val sw = (2.6f * s / 2.75f).coerceIn(1.8f, 3.4f)
    drawLine(PbColors.bikeFrame, rear, seat, strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(PbColors.bikeFrame, seat, front, strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(PbColors.bikeFrame, rear, pedal, strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(PbColors.bikeFrame, pedal, front, strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(
        PbColors.bikeFrameDark,
        seat,
        Offset(seat.x, seat.y - 4f * s),
        strokeWidth = sw * 0.9f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPelican(s: Float, wingDeg: Float) {
    val bodyC = Offset(0f, -18f * s)
    // belly
    drawOval(
        color = PbColors.pelicanBelly,
        topLeft = Offset(bodyC.x - 10f * s, bodyC.y - 4f * s),
        size = Size(18f * s, 14f * s),
    )
    // body
    drawOval(
        color = PbColors.pelicanBody,
        topLeft = Offset(bodyC.x - 14f * s, bodyC.y - 10f * s),
        size = Size(26f * s, 18f * s),
    )
    // wing
    withTransform({
        rotate(wingDeg, pivot = bodyC)
    }) {
        val wing = Path().apply {
            moveTo(bodyC.x - 2f * s, bodyC.y - 2f * s)
            quadraticTo(
                bodyC.x - 8f * s, bodyC.y - 22f * s,
                bodyC.x + 14f * s, bodyC.y - 10f * s,
            )
            quadraticTo(
                bodyC.x + 4f * s, bodyC.y - 4f * s,
                bodyC.x - 2f * s, bodyC.y - 2f * s,
            )
            close()
        }
        drawPath(wing, PbColors.pelicanBody.copy(alpha = 0.92f))
    }
    // head
    val head = Offset(bodyC.x + 14f * s, bodyC.y - 6f * s)
    drawCircle(PbColors.pelicanBody, radius = 7f * s, center = head)
    // eye
    drawCircle(PbColors.pelicanBelly, radius = 1.6f * s, center = Offset(head.x + 2f * s, head.y - 1f * s))
    drawCircle(PbColors.ink, radius = 0.7f * s, center = Offset(head.x + 2.4f * s, head.y - 1f * s))
    // beak
    val beak = Path().apply {
        moveTo(head.x + 5f * s, head.y - 1f * s)
        lineTo(head.x + 18f * s, head.y + 2f * s)
        lineTo(head.x + 5f * s, head.y + 4f * s)
        close()
    }
    drawPath(beak, PbColors.pelicanBeak)
}
