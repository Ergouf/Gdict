package io.github.gdict.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import io.github.gdict.ui.theme.GdictColors

/**
 * Experimental Android liquid-glass surface.
 *
 * Layers are intentionally separated:
 * 1. Haze samples and blurs content behind the control on Android 12+.
 * 2. API 33+ adds a low-amplitude AGSL caustic/specular field.
 * 3. Foreground content is drawn last and is never passed through the shader.
 * 4. Older Android versions keep the proven opaque v1.10.3 fallback.
 *
 * Haze 0.7.3 owns its edge background on the source modifier rather than HazeStyle.
 * GdictApp therefore supplies the opaque app background to the Haze source. Noise is
 * deliberately disabled here and at the source; Liquid Glass should look optically
 * clear rather than grainy/frosted.
 *
 * This does not yet displace the captured backdrop texture itself. The shader creates
 * the dynamic optical/highlight field while Haze owns backdrop sampling. Keeping those
 * concerns separate avoids distorting text/icons and gives us a safe fallback path.
 */
@Composable
fun LiquidGlassSurface(
    hazeState: HazeState?,
    darkMode: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val fallbackColor = if (darkMode) GdictColors.DarkGlassSurface else GdictColors.GlassSurface
    val borderColor = if (darkMode) GdictColors.DarkGlassBorder else GdictColors.GlassBorder
    val hazeTint = if (darkMode) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    val backdropEnabled = hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .then(
                if (backdropEnabled) {
                    Modifier.hazeChild(
                        state = hazeState!!,
                        shape = shape,
                        style = HazeStyle(
                            tint = hazeTint,
                            blurRadius = blurRadius,
                            noiseFactor = 0f
                        )
                    )
                } else {
                    Modifier.background(fallbackColor, shape)
                }
            )
            .border(0.6.dp, borderColor, shape)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidOpticsOverlay(
                darkMode = darkMode,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        } else if (backdropEnabled) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (darkMode) 0.07f else 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (darkMode) 0.025f else 0.01f)
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                )
            }
        }

        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LiquidOpticsOverlay(
    darkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "liquidGlassOptics")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquidGlassPhase"
    )

    val shader = remember { RuntimeShader(LIQUID_OPTICS_SHADER) }
    val brush = remember(shader) { RuntimeShaderBrush(shader) }
    val highlightAlpha = if (darkMode) 0.38f else 0.52f

    Canvas(modifier = modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("phase", phase)
        shader.setFloatUniform("strength", highlightAlpha)
        drawRect(brush = brush)

        drawRoundRect(
            color = Color.White.copy(alpha = if (darkMode) 0.11f else 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
            style = Stroke(width = 0.6.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(0.5.dp.toPx()))
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class RuntimeShaderBrush(
    private val runtimeShader: RuntimeShader
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        runtimeShader.setFloatUniform("resolution", size.width, size.height)
        return runtimeShader
    }
}

private const val LIQUID_OPTICS_SHADER = """
    uniform float2 resolution;
    uniform float phase;
    uniform float strength;

    half4 main(float2 p) {
        float2 uv = p / max(resolution, float2(1.0));
        float t = phase * 6.2831853;

        float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
        float rim = 1.0 - smoothstep(0.012, 0.13, edgeDistance);

        float waveA = sin((uv.x * 6.4 + uv.y * 2.2) * 6.2831853 + t);
        float waveB = sin((uv.y * 7.2 - uv.x * 1.9) * 6.2831853 - t * 0.68);
        float lens = 0.5 + 0.5 * waveA * waveB;

        float streakAxis = uv.x * 0.82 + uv.y * 0.34;
        float streakCenter = 0.5 + 0.16 * sin(t * 0.47);
        float streak = exp(-pow((streakAxis - streakCenter) * 9.0, 2.0));

        float alpha = strength * (rim * (0.040 + 0.028 * lens) + streak * 0.018);
        float coolShift = 0.006 * lens;
        return half4(1.0 - coolShift, 1.0, 1.0, alpha);
    }
"""
