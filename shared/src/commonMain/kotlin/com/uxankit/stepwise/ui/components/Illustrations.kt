package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
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

/** The green "step done" badge with a sparkle and a yellow dot. */
@Composable
fun DoneBadge(modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Box(modifier.size(150.dp, 130.dp).clearAndSetSemantics { }) {
        Box(
            Modifier.size(108.dp).align(Alignment.Center).clip(CircleShape).background(colors.grass),
            contentAlignment = Alignment.Center,
        ) {
            Icon(StepIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(48.dp))
        }
        Canvas(Modifier.size(24.dp).align(Alignment.TopEnd).offset((-4).dp, 6.dp)) {
            drawSparkle(colors.ink, center = center, outer = size.minDimension / 2)
        }
        Box(
            Modifier.size(12.dp).align(Alignment.BottomStart).offset(10.dp, (-24).dp).clip(CircleShape).background(colors.sunshine),
        )
    }
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
