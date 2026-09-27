package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** Small circle with an arrow, the action affordance inside every CTA (DESIGN.md). */
@Composable
fun ActionDot(background: Color, arrow: Color, size: Dp = 22.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(StepIcons.ArrowRight, contentDescription = null, tint = arrow, modifier = Modifier.size(14.dp))
    }
}

/** "Service CTA": the coral pill. The one main action on a screen. */
@Composable
fun ServiceCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.coral else colors.coral.copy(alpha = 0.45f))
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = StepwiseTheme.type.label, color = Color.White)
        Spacer(Modifier.width(8.dp))
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            ActionDot(background = Color.White, arrow = colors.coral)
        }
    }
}

/** "Primary CTA": a light ghost pill with a blue dot. Secondary to the coral action. */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    tall: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    showDot: Boolean = true,
) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (tall) 54.dp else 46.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.hairline), CircleShape)
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = StepwiseTheme.type.label, color = if (enabled) colors.ink else colors.muted)
        if (busy) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(Modifier.size(18.dp), color = colors.ink, strokeWidth = 2.dp)
        } else if (showDot) {
            Spacer(Modifier.width(8.dp))
            ActionDot(background = if (enabled) colors.sky else colors.hairline, arrow = Color.White)
        }
    }
}

/** 44dp white circle with a hairline border: back, close, more, profile. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
) {
    val colors = StepwiseTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.ink, modifier = Modifier.size(iconSize))
    }
}

/** "Inline Text Link": underlined, never a colour change. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = StepwiseTheme.colors.muted) {
    Text(
        text = text,
        style = StepwiseTheme.type.body.copy(textDecoration = TextDecoration.Underline),
        color = color,
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}
