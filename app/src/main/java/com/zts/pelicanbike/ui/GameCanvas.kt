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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zts.pelicanbike.game.ObstacleSkin
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
        val s = density
        drawSky()
        drawParallax(world, s, reduceMotion)
        drawGround(world, s)
        drawObstacles(world, s)
        drawPlayer(world, s, reduceMotion)
        drawDust(world, s, reduceMotion)
        drawPlusOne(world, s)
        if (world.flashAlpha > 0f) {
            drawRect(PbColors.dangerFlash.copy(alpha = world.flashAlpha))
        }
    }
}

/** Static pelican-on-bike silhouette for Start card (scale 0.55). */
@Composable
fun PelicanSilhouette(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    Canvas(modifier = modifier) {
        val s = density
        // Place ground-contact origin near bottom-center; scale 0.55
        val originX = size.width / 2f - 50f * s * 0.55f
        val originY = size.height * 0.95f
        withTransform({
            translate(originX, originY)
            scale(0.55f, 0.55f, pivot = Offset.Zero)
        }) {
            drawPlayerLayers(wheelAngle = 0f, wingDeg = 0f, s = s)
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
    val off2 = world.parallax2 * s * speedFactor
    val groundY = world.groundTop * s

    // L1 far hills
    val period0 = size.width * 0.55f
    for (i in -1..3) {
        val cx = i * period0 - (off0 % period0)
        drawOval(
            color = PbColors.parallaxDeep.copy(alpha = 0.95f),
            topLeft = Offset(cx, groundY - 100.dp.toPx()),
            size = Size(size.width * 0.48f, 130.dp.toPx()),
        )
        drawOval(
            color = PbColors.parallax.copy(alpha = 0.9f),
            topLeft = Offset(cx + size.width * 0.12f, groundY - 78.dp.toPx()),
            size = Size(size.width * 0.42f, 105.dp.toPx()),
        )
    }

    // L2 clouds
    val period1 = size.width * 0.48f
    val cloudYs = floatArrayOf(0.14f, 0.22f, 0.18f, 0.26f)
    for (i in -1..4) {
        val base = i * period1 - (off1 % period1)
        val cy = size.height * cloudYs[(i + 4) % cloudYs.size]
        drawCloud(base + 20.dp.toPx(), cy, 0.62f)
        if (i % 2 == 0) {
            drawCloud(base + period1 * 0.45f, cy + 10.dp.toPx(), 0.55f)
        }
    }

    // L3 near silhouettes
    val period2 = 220.dp.toPx()
    var sx = -((off2) % period2) - period2
    var idx = 0
    while (sx < size.width + period2) {
        val h = (28f + (idx % 3) * 8f).dp.toPx()
        val alpha = 0.35f + (idx % 2) * 0.08f
        if (idx % 3 == 1) {
            drawLine(
                color = PbColors.nearSilhouette.copy(alpha = alpha),
                start = Offset(sx, groundY),
                end = Offset(sx, groundY - h),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = PbColors.nearSilhouette.copy(alpha = alpha),
                start = Offset(sx, groundY - h),
                end = Offset(sx + 10.dp.toPx(), groundY - h + 2.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else {
            drawLine(
                color = PbColors.nearSilhouette.copy(alpha = alpha),
                start = Offset(sx, groundY),
                end = Offset(sx, groundY - h * 0.45f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val canopy = Path().apply {
                moveTo(sx, groundY - h)
                lineTo(sx - 12.dp.toPx(), groundY - h * 0.4f)
                lineTo(sx + 12.dp.toPx(), groundY - h * 0.4f)
                close()
            }
            drawPath(canopy, PbColors.nearSilhouette.copy(alpha = alpha))
        }
        sx += period2
        idx++
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, alpha: Float) {
    val c = PbColors.card.copy(alpha = alpha)
    drawOval(c, topLeft = Offset(x, y), size = Size(64.dp.toPx(), 22.dp.toPx()))
    drawOval(c, topLeft = Offset(x + 18.dp.toPx(), y - 8.dp.toPx()), size = Size(40.dp.toPx(), 18.dp.toPx()))
    drawOval(c, topLeft = Offset(x + 36.dp.toPx(), y + 2.dp.toPx()), size = Size(34.dp.toPx(), 16.dp.toPx()))
}

private fun DrawScope.drawGround(world: WorldSnapshot, s: Float) {
    val groundY = world.groundTop * s
    drawRect(
        color = PbColors.ground,
        topLeft = Offset(0f, groundY),
        size = Size(size.width, size.height - groundY),
    )
    drawRect(
        color = PbColors.groundShadow,
        topLeft = Offset(0f, groundY - 1.dp.toPx()),
        size = Size(size.width, 1.dp.toPx()),
    )
    drawRect(
        color = PbColors.groundLine,
        topLeft = Offset(0f, groundY),
        size = Size(size.width, 1.5.dp.toPx()),
    )
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

private fun DrawScope.drawObstacles(world: WorldSnapshot, s: Float) {
    val pipeW = Physics.PipeWidth * s
    val visualPad = 3.dp.toPx()
    val groundY = world.groundTop * s
    for (p in world.pipes) {
        val x = p.x * s
        val gapTop = (p.gapCenterY - p.gapHeight / 2f) * s
        val gapBot = (p.gapCenterY + p.gapHeight / 2f) * s
        val vx = x - visualPad
        val vw = pipeW + visualPad * 2f
        when (p.skin) {
            ObstacleSkin.Barricade -> {
                if (gapTop > 0f) drawBarricade(vx, 0f, vw, gapTop, topCap = true)
                val botH = groundY - gapBot
                if (botH > 0f) drawBarricade(vx, gapBot, vw, botH, topCap = false)
            }
            ObstacleSkin.Arch -> {
                if (gapTop > 0f) drawArchColumn(vx, 0f, vw, gapTop, atGapBottom = true)
                val botH = groundY - gapBot
                if (botH > 0f) drawArchColumn(vx, gapBot, vw, botH, atGapBottom = false)
            }
            ObstacleSkin.Stump -> {
                if (gapTop > 0f) drawStump(vx, 0f, vw, gapTop, ringsAtBottom = true)
                val botH = groundY - gapBot
                if (botH > 0f) drawStump(vx, gapBot, vw, botH, ringsAtBottom = false)
            }
        }
    }
}

private fun DrawScope.drawBarricade(x: Float, y: Float, w: Float, h: Float, topCap: Boolean) {
    val r = 10.dp.toPx()
    drawRoundRect(
        color = PbColors.obstacle,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r),
    )
    val bevelH = minOf(10.dp.toPx(), h * 0.25f)
    if (topCap) {
        drawRoundRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x, y + h - bevelH),
            size = Size(w, bevelH),
            cornerRadius = CornerRadius(r / 2f),
        )
    } else {
        drawRoundRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x, y),
            size = Size(w, bevelH),
            cornerRadius = CornerRadius(r / 2f),
        )
    }
    val stripeH = 7.dp.toPx()
    val midY = y + h * 0.35f
    for (i in 0 until 4) {
        val sy = midY + i * stripeH
        if (sy + stripeH > y + h - 4.dp.toPx()) break
        val color = if (i % 2 == 0) PbColors.blue else PbColors.pelicanBelly
        drawRect(color, topLeft = Offset(x + 4.dp.toPx(), sy), size = Size(w - 8.dp.toPx(), stripeH))
    }
    drawRect(
        color = PbColors.bikeFrameDark,
        topLeft = Offset(x, y + h - 1.5.dp.toPx()),
        size = Size(w, 1.5.dp.toPx()),
    )
}

private fun DrawScope.drawArchColumn(x: Float, y: Float, w: Float, h: Float, atGapBottom: Boolean) {
    val r = 8.dp.toPx()
    drawRoundRect(
        color = PbColors.obstacle,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r),
    )
    drawLine(
        color = PbColors.bikeFrameDark.copy(alpha = 0.4f),
        start = Offset(x + w * 0.5f, y + 6.dp.toPx()),
        end = Offset(x + w * 0.5f, y + h - 6.dp.toPx()),
        strokeWidth = 1.5.dp.toPx(),
    )
    val beamH = minOf(16.dp.toPx(), h * 0.35f)
    if (atGapBottom) {
        drawRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x - 2.dp.toPx(), y + h - beamH),
            size = Size(w + 4.dp.toPx(), beamH),
        )
        drawRect(
            color = PbColors.blue,
            topLeft = Offset(x - 2.dp.toPx(), y + h - 2.dp.toPx()),
            size = Size(w + 4.dp.toPx(), 2.dp.toPx()),
        )
    } else {
        drawRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x - 2.dp.toPx(), y),
            size = Size(w + 4.dp.toPx(), beamH),
        )
        drawRect(
            color = PbColors.blue,
            topLeft = Offset(x - 2.dp.toPx(), y),
            size = Size(w + 4.dp.toPx(), 2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawStump(x: Float, y: Float, w: Float, h: Float, ringsAtBottom: Boolean) {
    val r = 12.dp.toPx()
    if (!ringsAtBottom) {
        drawRoundRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x - 2.dp.toPx(), y + h - 8.dp.toPx()),
            size = Size(w + 4.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }
    drawRoundRect(
        color = PbColors.obstacle,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r),
    )
    drawRect(
        color = PbColors.obstacleMid,
        topLeft = Offset(x + 3.dp.toPx(), y + 6.dp.toPx()),
        size = Size(2.dp.toPx(), maxOf(0f, h - 12.dp.toPx())),
    )
    val ringCy = if (ringsAtBottom) y + h - 2.dp.toPx() else y + 2.dp.toPx()
    val rx = w * 0.38f
    val ry = minOf(10.dp.toPx(), h * 0.2f)
    drawOval(
        color = PbColors.stumpRing.copy(alpha = 0.9f),
        topLeft = Offset(x + w * 0.5f - rx, ringCy - ry),
        size = Size(rx * 2f, ry * 2f),
    )
    drawOval(
        color = PbColors.wheelSpoke,
        topLeft = Offset(x + w * 0.5f - rx * 0.7f, ringCy - ry * 0.7f),
        size = Size(rx * 1.4f, ry * 1.4f),
        style = Stroke(width = 1.dp.toPx()),
    )
    drawOval(
        color = PbColors.wheelSpoke,
        topLeft = Offset(x + w * 0.5f - rx * 0.4f, ringCy - ry * 0.4f),
        size = Size(rx * 0.8f, ry * 0.8f),
        style = Stroke(width = 1.dp.toPx()),
    )
    if (!ringsAtBottom) {
        drawRoundRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x - 3.dp.toPx(), y + h - 5.dp.toPx()),
            size = Size(8.dp.toPx(), 5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = PbColors.obstacleMid,
            topLeft = Offset(x + w - 4.dp.toPx(), y + h - 4.dp.toPx()),
            size = Size(7.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawPlayer(world: WorldSnapshot, s: Float, reduceMotion: Boolean) {
    val p = world.player
    val bobAmp = if (reduceMotion) 0f else {
        val flying = kotlin.math.abs(p.vy) > 40f
        if (flying) 1.25f else 2.5f
    }
    val bob = sin(p.bobPhase.toDouble()).toFloat() * bobAmp * s
    // Bbox center → rear-wheel ground-contact origin (100×76, origin bottom of bbox)
    val originX = p.x * s - 50f * s + world.shakeX * s
    val originY = p.y * s + 38f * s + bob
    val wingTilt = when {
        reduceMotion -> 0f
        p.vy < -50f -> -10f
        p.vy > 80f -> 6f
        else -> 0f
    }
    withTransform({
        translate(originX, originY)
    }) {
        drawPlayerLayers(p.wheelAngle, wingTilt, s)
    }
}

/**
 * Layered pelican-on-bike. Local origin = rear-wheel ground contact.
 * Spec Y-up-negative matches canvas (+Y down): use anchors as written.
 */
private fun DrawScope.drawPlayerLayers(
    wheelAngle: Float,
    wingDeg: Float,
    s: Float,
) {
    val R = Offset(18f * s, -16f * s)
    val F = Offset(78f * s, -16f * s)
    val BB = Offset(48f * s, -16f * s)
    val ST = Offset(40f * s, -42f * s)
    val HD = Offset(70f * s, -40f * s)
    val BODY = Offset(46f * s, -52f * s)
    val HEAD = Offset(68f * s, -58f * s)

    // z1 rear wheel
    drawFancyWheel(R, 16f * s, wheelAngle)
    // z2 frame
    drawDiamondFrame(R, F, BB, ST, HD, wheelAngle, s)
    // z3 front wheel
    drawFancyWheel(F, 15f * s, wheelAngle * 1.02f)
    // z4 body
    drawOval(
        color = PbColors.pelicanBody,
        topLeft = Offset(BODY.x - 16f * s, BODY.y - 11f * s),
        size = Size(32f * s, 22f * s),
    )
    drawOval(
        color = PbColors.pelicanBelly,
        topLeft = Offset(BODY.x - 12f * s, BODY.y - 4.5f * s),
        size = Size(22f * s, 15f * s),
    )
    // z5 wings
    val pivot = Offset(46f * s, -56f * s)
    withTransform({
        rotate(wingDeg, pivot = pivot)
    }) {
        // Spec paths use Y-up-negative already
        val under = Path().apply {
            moveTo(40f * s, -50f * s)
            quadraticTo(28f * s, -72f * s, 58f * s, -64f * s)
            quadraticTo(48f * s, -54f * s, 40f * s, -50f * s)
            close()
        }
        drawPath(under, PbColors.wingUnder)
        val over = Path().apply {
            moveTo(42f * s, -52f * s)
            quadraticTo(32f * s, -68f * s, 56f * s, -60f * s)
            quadraticTo(48f * s, -54f * s, 42f * s, -52f * s)
            close()
        }
        drawPath(over, PbColors.pelicanBody)
    }
    // z6 head / pouch / beak / eye
    drawCircle(PbColors.pelicanBody, radius = 8f * s, center = HEAD)
    val pouch = Path().apply {
        moveTo(HEAD.x + 5f * s, HEAD.y + 1f * s)
        quadraticTo(HEAD.x + 22f * s, HEAD.y + 4f * s, HEAD.x + 18f * s, HEAD.y + 14f * s)
        quadraticTo(HEAD.x + 8f * s, HEAD.y + 12f * s, HEAD.x + 4f * s, HEAD.y + 5f * s)
        close()
    }
    drawPath(pouch, PbColors.pelicanPouch)
    drawLine(
        color = PbColors.pelicanBeak,
        start = Offset(HEAD.x + 5f * s, HEAD.y + 1f * s),
        end = Offset(HEAD.x + 18f * s, HEAD.y + 4f * s),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val beak = Path().apply {
        moveTo(HEAD.x + 6f * s, HEAD.y - 1f * s)
        lineTo(HEAD.x + 20f * s, HEAD.y + 1f * s)
        lineTo(HEAD.x + 6f * s, HEAD.y + 3f * s)
        close()
    }
    drawPath(beak, PbColors.pelicanBeak)
    drawCircle(PbColors.pelicanBelly, radius = 2.4f * s, center = Offset(72f * s, -60f * s))
    drawCircle(PbColors.ink, radius = 1.1f * s, center = Offset(73f * s, -60f * s))
    drawCircle(PbColors.card, radius = 0.7f * s, center = Offset(73.6f * s, -60.7f * s))
    // z7 legs
    drawLine(
        PbColors.pelicanBody,
        Offset(BODY.x - 2f * s, BODY.y + 10f * s),
        Offset(BB.x - 2f * s, BB.y - 2f * s),
        strokeWidth = 1.8f * s,
        cap = StrokeCap.Round,
    )
    drawLine(
        PbColors.pelicanBody,
        Offset(BODY.x + 2f * s, BODY.y + 10f * s),
        Offset(BB.x + 2f * s, BB.y - 2f * s),
        strokeWidth = 1.8f * s,
        cap = StrokeCap.Round,
    )
    drawCircle(PbColors.bikeFrameDark, radius = 1.2f * s, center = Offset(BB.x - 2f * s, BB.y - 1f * s))
    drawCircle(PbColors.bikeFrameDark, radius = 1.2f * s, center = Offset(BB.x + 2f * s, BB.y - 1f * s))
}

private fun DrawScope.drawFancyWheel(center: Offset, radius: Float, angle: Float) {
    val strokeTire = maxOf(3.5f, radius * 0.28f)
    drawCircle(
        color = PbColors.wheelTire,
        radius = radius,
        center = center,
        style = Stroke(width = strokeTire),
    )
    drawCircle(
        color = PbColors.border,
        radius = radius * 0.75f,
        center = center,
        style = Stroke(width = maxOf(1.2f, radius * 0.08f)),
    )
    val spokes = 10
    for (i in 0 until spokes) {
        val a = angle + i * (Math.PI.toFloat() * 2f / spokes)
        val dx = cos(a) * radius * 0.88f
        val dy = sin(a) * radius * 0.88f
        drawLine(
            color = PbColors.wheelSpoke,
            start = center,
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = maxOf(1.4f, radius * 0.09f),
            cap = StrokeCap.Round,
        )
    }
    val hubR = maxOf(2.5f, radius * 0.16f)
    drawCircle(PbColors.blue, radius = hubR, center = center)
    drawCircle(
        color = PbColors.bikeFrameDark,
        radius = hubR,
        center = center,
        style = Stroke(width = 0.8f),
    )
}

private fun DrawScope.drawDiamondFrame(
    R: Offset,
    F: Offset,
    BB: Offset,
    ST: Offset,
    HD: Offset,
    wheelAngle: Float,
    s: Float,
) {
    val dark = 5f * s
    val blue = 3.2f * s
    fun tube(a: Offset, b: Offset) {
        drawLine(PbColors.bikeFrameDark, a, b, strokeWidth = dark, cap = StrokeCap.Round)
        drawLine(PbColors.bikeFrame, a, b, strokeWidth = blue, cap = StrokeCap.Round)
    }
    val fInner = Offset(74f * s, -28f * s)
    tube(BB, ST)
    tube(ST, HD)
    tube(HD, fInner)
    tube(fInner, BB)
    tube(Offset(18f * s, -28f * s), BB)
    tube(BB, Offset(78f * s, -28f * s))
    val seat = Path().apply {
        moveTo(34f * s, -46f * s)
        lineTo(46f * s, -46f * s)
        lineTo(44f * s, -42f * s)
        lineTo(36f * s, -42f * s)
        close()
    }
    drawPath(seat, PbColors.ink)
    drawLine(
        PbColors.bikeFrameDark,
        Offset(62f * s, -40f * s),
        Offset(78f * s, -40f * s),
        strokeWidth = 2.2f * s,
        cap = StrokeCap.Round,
    )
    drawLine(
        PbColors.bikeFrameDark,
        Offset(70f * s, -40f * s),
        Offset(70f * s, -34f * s),
        strokeWidth = 2.2f * s,
        cap = StrokeCap.Round,
    )
    val crankLen = 5f * s
    val ca = wheelAngle * 2f
    drawLine(
        PbColors.bikeFrameDark,
        Offset(BB.x - cos(ca) * crankLen, BB.y - sin(ca) * crankLen),
        Offset(BB.x + cos(ca) * crankLen, BB.y + sin(ca) * crankLen),
        strokeWidth = 1.6f * s,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawDust(world: WorldSnapshot, s: Float, reduceMotion: Boolean) {
    if (reduceMotion) return
    for (d in world.dust) {
        val t = (d.age / d.life).coerceIn(0f, 1f)
        val alpha = 0.85f * (1f - t)
        val radius = (2.5f - 1.5f * t).dp.toPx()
        drawCircle(
            color = PbColors.dust.copy(alpha = alpha),
            radius = radius,
            center = Offset(d.x * s + world.shakeX * s, d.y * s),
        )
    }
}

private fun DrawScope.drawPlusOne(world: WorldSnapshot, s: Float) {
    val fx = world.plusOne ?: return
    val t = (fx.age / fx.life).coerceIn(0f, 1f)
    val alpha = 1f - t
    val lift = -12f * s * t
    val x = fx.x * s + world.shakeX * s
    val y = fx.y * s + lift
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                0x25, 0x63, 0xEB,
            )
            textSize = 14.dp.toPx()
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("+1", x, y, paint)
    }
}
