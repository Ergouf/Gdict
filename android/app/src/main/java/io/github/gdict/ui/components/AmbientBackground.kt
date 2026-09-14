package io.github.gdict.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility shim for screens that have not reached their migration phase.
 *
 * Apple HIG-inspired content surfaces stay quiet and neutral. The previous
 * implementation painted four blue radial glows behind every screen, which
 * made decorative material compete with dictionary content. Migrated screens
 * should stop calling this helper entirely; until then it intentionally adds
 * no visual effect.
 */
@Composable
fun Modifier.acrylicAmbientBackground(
    darkMode: Boolean = false,
    screenWidthPx: Float,
    screenHeightPx: Float
): Modifier = this
