package io.github.gdict.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val ENTER_DURATION = 250
private const val STAGGER_INTERVAL = 50
private const val MAX_STAGGER_ITEMS = 8

/**
 * 页面进入动效：Fade + 上浮 16dp（250ms）。
 */
@Composable
fun Modifier.pageEnterAnimation(): Modifier {
    val alphaAnim = remember { Animatable(0f) }
    val offsetY = remember { Animatable(16f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(ENTER_DURATION))
    }
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, tween(ENTER_DURATION))
    }
    return this
        .alpha(alphaAnim.value)
        .graphicsLayer { translationY = offsetY.value.dp.toPx() }
}

/**
 * 列表项 Stagger 淡入：按 index 延迟入场（间隔 50ms，最多 8 项后不再延迟）。
 */
@Composable
fun Modifier.staggerEnterAnimation(index: Int): Modifier {
    val alphaAnim = remember { Animatable(0f) }
    val offsetY = remember { Animatable(16f) }
    val delay = index.coerceAtMost(MAX_STAGGER_ITEMS) * STAGGER_INTERVAL
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(ENTER_DURATION, delayMillis = delay))
    }
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, tween(ENTER_DURATION, delayMillis = delay))
    }
    return this
        .alpha(alphaAnim.value)
        .graphicsLayer { translationY = offsetY.value.dp.toPx() }
}

/**
 * 点击缩放反馈：按下时缩放到 [scaleDown]。
 */
@Composable
fun Modifier.pressScale(
    pressed: Boolean,
    scaleDown: Float = 0.98f
): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        scale.animateTo(if (pressed) scaleDown else 1f, tween(120))
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
