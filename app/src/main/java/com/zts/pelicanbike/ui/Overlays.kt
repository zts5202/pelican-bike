package com.zts.pelicanbike.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.pelicanbike.game.WorldSnapshot

private val EnterEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

@Composable
fun StartOverlay(
    world: WorldSnapshot,
    onStart: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = if (world.muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (world.muted) "取消静音" else "静音",
                tint = PbColors.ink,
                modifier = Modifier.size(22.dp),
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(280, easing = EnterEasing)) +
                scaleIn(tween(280, easing = EnterEasing), initialScale = 0.96f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CardShell {
                Text(
                    text = "鹈鹕骑车",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    color = PbColors.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点按跳跃 · 躲开柱子",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = PbColors.inkMuted,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton(text = "开始", onClick = onStart)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "最高分 ${world.highScore}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PbColors.inkMuted,
                )
            }
        }
    }
}

@Composable
fun PlayingHud(
    world: WorldSnapshot,
    scorePulse: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (scorePulse) 1.12f else 1f,
        animationSpec = tween(200),
        label = "scorePulse",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp),
    ) {
        Text(
            text = "${world.score}",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = PbColors.ink,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.TopStart)
                .scale(scale),
        )
        Text(
            text = "最佳 ${world.highScore}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = PbColors.inkMuted,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
fun GameOverOverlay(
    world: WorldSnapshot,
    onRetry: () -> Unit,
    onTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PbColors.scrim),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(280, easing = EnterEasing)) +
                scaleIn(tween(280, easing = EnterEasing), initialScale = 0.94f),
        ) {
            CardShell {
                Text(
                    text = "本局",
                    fontSize = 14.sp,
                    color = PbColors.inkMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${world.score}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PbColors.ink,
                    fontFamily = FontFamily.SansSerif,
                )
                if (world.isNewRecord) {
                    Spacer(Modifier.height(10.dp))
                    NewRecordBadge()
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "再来一次", onClick = onRetry)
                Spacer(Modifier.height(12.dp))
                GhostButton(text = "回标题", onClick = onTitle)
            }
        }
    }
}

@Composable
private fun NewRecordBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PbColors.blue.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = PbColors.blue,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "新纪录",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PbColors.blue,
            )
        }
    }
}

@Composable
private fun CardShell(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = PbColors.ink.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(20.dp))
            .background(PbColors.card)
            .border(1.dp, PbColors.border, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) tween(80) else spring(
            dampingRatio = 0.7f,
            stiffness = 400f,
        ),
        label = "primaryPress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) PbColors.bluePressed else PbColors.blue)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PbColors.card,
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) tween(80) else spring(
            dampingRatio = 0.7f,
            stiffness = 400f,
        ),
        label = "ghostPress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(PbColors.card)
            .border(1.5.dp, PbColors.border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = PbColors.ink,
        )
    }
}
