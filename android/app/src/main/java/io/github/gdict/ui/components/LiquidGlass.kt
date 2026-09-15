package io.github.gdict.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gdict.ui.theme.GdictColors

/**
 * Experimental liquid-glass surface backed only by Compose GraphicsLayer + Android RenderEffect.
 *
 * There is deliberately no Haze dependency here. The app records the page into [backdropLayer].
 * This surface crops the matching region into its own graphics layer, applies blur/refraction to
 * that copy, and finally draws foreground controls normally. Icons/text therefore never pass
 * through the optical shader.
 *
 * Android 13+: backdrop blur + true AGSL displacement sampling.
 * Android 12/12L: backdrop blur only.
 * Android 11 and below (and previews without a captured layer): stable opaque v1.10.3 fallback.
 */
@Composable
fun LiquidGlassSurface(
    backdropLayer: GraphicsLayer?,
    darkMode: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val fallbackColor = if (darkMode) GdictColors.DarkGlassSurface else GdictColors.GlassSurface
    val borderColor = if (darkMode) GdictColors.DarkGlassBorder else GdictColors.GlassBorder
    val glassLayer = rememberGraphicsLayer()
    var rootPosition by remember { mutableStateOf(Offset.Zero) }

    val transition = rememberInfiniteTransition(label = "liquidGlassRefraction")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquidGlassRefractionPhase"
    )

    val runtimeShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(LIQUID_REFRACTION_SHADER) }
    } else {
        null
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { rootPosition = it.positionInRoot() }
            .then(
                if (backdropLayer == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Modifier.background(fallbackColor, shape)
                } else {
                    Modifier.drawWithContent {
                        val width = size.width.coerceAtLeast(1f)
                        val height = size.height.coerceAtLeast(1f)

                        // Record only the pixels that geometrically sit behind this surface.
                        // The negative translation aligns the full-page recording to local coords.
                        glassLayer.record {
                            withTransform({
                                translate(-rootPosition.x, -rootPosition.y)
                            }) {
                                drawLayer(backdropLayer)
                            }
                        }

                        val radiusPx = blurRadius.toPx()
                        glassLayer.renderEffect = if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            runtimeShader != null
                        ) {
                            runtimeShader.setFloatUniform("resolution", width, height)
                            runtimeShader.setFloatUniform("phase", phase)
                            runtimeShader.setFloatUniform("amplitude", 2.4f)

                            val blur = AndroidRenderEffect.createBlurEffect(
                                radiusPx,
                                radiusPx,
                                AndroidShader.TileMode.CLAMP
                            )
                            val refraction = AndroidRenderEffect.createRuntimeShaderEffect(
                                runtimeShader,
                                "backdrop"
                            )
                            AndroidRenderEffect.createChainEffect(refraction, blur)
                                .asComposeRenderEffect()
                        } else {
                            AndroidRenderEffect.createBlurEffect(
                                radiusPx,
                                radiusPx,
                                AndroidShader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }

                        // The captured layer is the background only. Foreground content is drawn
                        // afterwards and therefore remains pixel-perfect and undistorted.
                        drawLayer(glassLayer)

                        // A tiny neutral tint improves contrast without turning the material gray.
                        drawRect(
                            color = if (darkMode) {
                                Color.Black.copy(alpha = 0.10f)
                            } else {
                                Color.White.copy(alpha = 0.09f)
                            }
                        )
                        drawContent()
                    }
                }
            )
            .border(0.65.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Samples the actual captured backdrop. No grain and no full-width highlight streaks: those made
 * the previous Haze prototypes look dirty and also made seam diagnosis ambiguous.
 */
private const val LIQUID_REFRACTION_SHADER = """
    uniform shader backdrop;
    uniform float2 resolution;
    uniform float phase;
    uniform float amplitude;

    half4 main(float2 p) {
        float2 safeResolution = max(resolution, float2(1.0));
        float2 uv = p / safeResolution;
        float t = phase * 6.2831853;

        // Lens field grows only near the rounded surface boundary. Keep displacement very small
        // in the center so text/content under the glass remains recognizable.
        float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
        float edgeField = 1.0 - smoothstep(0.035, 0.22, edge);

        float waveX = sin(uv.y * 10.0 + t) * 0.55 + sin(uv.x * 4.0 - t * 0.61) * 0.45;
        float waveY = cos(uv.x * 9.0 - t * 0.83) * 0.55 + cos(uv.y * 3.5 + t * 0.47) * 0.45;
        float2 displacement = float2(waveX, waveY) * amplitude * (0.22 + 0.78 * edgeField);

        // Clamp sample coordinates so the shader never asks for pixels outside this local layer.
        float2 margin = float2(2.5);
        float2 samplePoint = clamp(p + displacement, margin, safeResolution - margin);
        half4 c = backdrop.eval(samplePoint);

        // Subtle lens lift at the perimeter, not a horizontal streak.
        float rim = edgeField * 0.035;
        c.rgb = mix(c.rgb, half3(1.0), rim);
        return c;
    }
"""
