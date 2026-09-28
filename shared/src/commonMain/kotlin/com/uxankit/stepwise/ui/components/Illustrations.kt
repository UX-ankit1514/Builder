package com.uxankit.stepwise.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ART_W = 299f
private const val ART_H = 230f

/**
 * "Illustration/Steps" from Figma: four rising bars, a dot for "you" on the third
 * step and a sparkle. Geometry is taken from the 299×230 component.
 */
@Composable
fun StepsIllustration(modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Canvas(modifier.aspectRatio(ART_W / ART_H).clearAndSetSemantics { }) {
        val s = size.width / ART_W
        val radius = CornerRadius(20.7f * s)
        val bars = listOf(
            Triple(0f, 64f, colors.grass),
            Triple(76f, 110f, colors.sky),
            Triple(152f, 156f, colors.coral),
            Triple(228f, 202f, colors.sunshine),
        )
        bars.forEach { (x, h, color) ->
            drawRoundRect(color, topLeft = Offset(x * s, (ART_H - h) * s), size = Size(67f * s, h * s), cornerRadius = radius)
        }
        drawCircle(colors.ink, radius = 19.5f * s, center = Offset((166f + 19.5f) * s, (25f + 19.5f) * s))
        drawSparkle(colors.ink, center = Offset((262f + 12.5f) * s, 12.5f * s), outer = 12.5f * s)
    }
}

/**
 * The green "step done" badge with a sparkle and a yellow dot. On arrival it plays the
 * transitions.dev success check: the badge fades in, turns upright, un-blurs and bobs into
 * place while the check draws itself; then the sparkle and the dot pop in.
 */
@Composable
fun DoneBadge(modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val start = if (reduceMotion) 1f else 0f
    val appear = remember { Animatable(start) }
    val bob = remember { Animatable(start) }
    val draw = remember { Animatable(start) }
    val sparkle = remember { Animatable(start) }
    val dot = remember { Animatable(start) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        launch { appear.animateTo(1f, tween(Motion.VERY_SLOW, easing = Motion.SmoothOut)) }
        launch { bob.animateTo(1f, tween(Motion.VERY_SLOW, easing = Motion.Bounce)) }
        launch { draw.animateTo(1f, tween(Motion.VERY_SLOW, delayMillis = Motion.MICRO, easing = Motion.SmoothOut)) }
        launch { sparkle.animateTo(1f, tween(Motion.VERY_SLOW, delayMillis = Motion.FAST, easing = Motion.Bounce)) }
        launch { dot.animateTo(1f, tween(Motion.VERY_SLOW, delayMillis = Motion.FAST + Motion.MICRO, easing = Motion.Bounce)) }
    }
    val density = LocalDensity.current
    val bobDistance = with(density) { Motion.DistanceCheckBob.toPx() }
    val blur = with(density) { Motion.BlurLarge.toPx() }

    Box(modifier.size(150.dp, 130.dp).clearAndSetSemantics { }) {
        Box(
            Modifier
                .size(108.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    val remaining = 1f - appear.value
                    alpha = appear.value
                    rotationZ = Motion.CHECK_ROTATE * remaining
                    translationY = bobDistance * (1f - bob.value)
                    val r = blur * remaining
                    renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
                }
                .clip(CircleShape)
                .background(colors.grass),
            contentAlignment = Alignment.Center,
        ) {
            DrawnCheck({ draw.value }, colors.ink, Modifier.size(48.dp))
        }
        Canvas(Modifier.size(24.dp).align(Alignment.TopEnd).offset((-4).dp, 6.dp).popIn { sparkle.value }) {
            drawSparkle(colors.ink, center = center, outer = size.minDimension / 2)
        }
        Box(
            Modifier.size(12.dp).align(Alignment.BottomStart).offset(10.dp, (-24).dp)
                .popIn { dot.value }
                .clip(CircleShape).background(colors.sunshine),
        )
    }
}

/** Grows from a quarter size and fades in as [progress] runs 0..1 (overshoot allowed). */
private fun Modifier.popIn(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    val scale = Motion.ICON_SCALE + (1f - Motion.ICON_SCALE) * p
    scaleX = scale
    scaleY = scale
    alpha = p.coerceIn(0f, 1f)
}

private fun DrawScope.drawSparkle(color: Color, center: Offset, outer: Float) {
    val inner = outer * 0.28f
    val path = Path()
    for (i in 0 until 8) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 4
        val point = Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color)
}
