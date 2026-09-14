package io.github.gdict.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility motion helpers retained while screens migrate phase by phase.
 *
 * Global page lift, list staggering, and generic press scaling made ordinary
 * dictionary navigation feel animated for animation's sake. Migrated screens
 * should use transitions only when they explain a state change (for example,
 * the flashcard reveal) rather than automatically animating every item.
 */
@Composable
fun Modifier.pageEnterAnimation(): Modifier = this

@Composable
fun Modifier.staggerEnterAnimation(index: Int): Modifier = this

@Composable
fun Modifier.pressScale(
    pressed: Boolean,
    scaleDown: Float = 0.98f
): Modifier = this
