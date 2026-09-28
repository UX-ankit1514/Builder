package com.uxankit.stepwise.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CardShape = RoundedCornerShape(50.dp)
val GroupShape = RoundedCornerShape(30.dp)

/** "Content Card": white on cream, no shadow. Elevation comes from the surface change alone. */
@Composable
fun StepCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    color: Color = StepwiseTheme.colors.surface,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    spacing: Int = 10,
    onClick: (() -> Unit)? = null,
    sound: UiSound = UiSound.Navigate,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressClickable(shape, sound = sound, onClick = onClick) else Modifier.clip(shape))
            .fillMaxWidth()
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** Round "mark done" toggle used on task and step rows. The box fills, then the check draws in, with a pop. */
@Composable
fun CheckCircle(checked: Boolean, onToggle: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val haptics = LocalHapticFeedback.current
    val hapticsOn = StepwiseTheme.settings.haptics
    val boxSpec = tween<Color>(Motion.CHECK_BOX, easing = Motion.SmoothOut)
    val fill by animateColorAsState(if (checked) colors.grass else colors.grass.copy(alpha = 0f), boxSpec, label = "check fill")
    val ring by animateColorAsState(if (checked) colors.grass else colors.ink, boxSpec, label = "check ring")
    Box(
        modifier = modifier
            .size(44.dp)
            .pressClickable(CircleShape, role = Role.Checkbox, sound = if (checked) UiSound.ToggleOff else UiSound.Pop) {
                if (hapticsOn) haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                onToggle()
            }
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "Done" else "Not done"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CheckMark(checked, colors.ink, Modifier.size(14.dp))
        }
    }
}

/** A done toggle waiting for its tick to finish drawing. */
private class PendingToggle {
    var job: Job? = null
}

/**
 * "Task Row": check circle, title, optional sub-line, chevron.
 * Ticking shows at once, but [onToggle] runs only after the check has drawn, so the row doesn't
 * leave its list mid-animation. A second tap in that moment takes it back.
 */
@Composable
fun TaskRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    sound: UiSound = UiSound.Navigate,
) {
    val colors = StepwiseTheme.colors
    val type = StepwiseTheme.type
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val scope = rememberCoroutineScope()
    val latestToggle by rememberUpdatedState(onToggle)
    val pending = remember { PendingToggle() }
    var shownChecked by remember(checked) { mutableStateOf(checked) }
    DisposableEffect(Unit) {
        onDispose {
            // Leaving the screen mid-tick commits it, so the tap is never lost.
            if (pending.job != null) {
                pending.job?.cancel()
                pending.job = null
                latestToggle()
            }
        }
    }
    fun toggle() {
        pending.job?.let { waiting ->
            waiting.cancel()
            pending.job = null
            shownChecked = checked
            return
        }
        shownChecked = !checked
        if (reduceMotion) {
            onToggle()
            return
        }
        pending.job = scope.launch {
            delay(Motion.VERY_SLOW.toLong())
            pending.job = null
            latestToggle()
        }
    }
    val titleColor by animateColorAsState(
        if (shownChecked) colors.muted else colors.ink,
        tween(Motion.QUICK, easing = Motion.SmoothOut),
        label = "task title",
    )
    Row(
        modifier = modifier
            .pressClickable(CardShape, sound = sound, onClick = onClick)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surface)
            .then(if (border != null) Modifier.border(border, CardShape) else Modifier)
            .padding(start = 4.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = shownChecked, onToggle = ::toggle, label = "Mark “$title” done")
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = type.bodyLargeMedium.copy(fontWeight = type.body.fontWeight),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, style = type.small, color = colors.muted)
        }
        Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
    }
}

/**
 * A numbered step circle: grass with a check when done, ink when current, outlined otherwise.
 * Finishing a step fills it and draws the check while the number shrinks away; the current
 * step grows to [currentSize].
 */
@Composable
fun StepBubble(
    number: Int,
    done: Boolean,
    current: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    currentSize: Dp = size,
    borderWidth: Dp = 1.dp,
) {
    val colors = StepwiseTheme.colors
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val boxSpec = tween<Color>(Motion.CHECK_BOX, easing = Motion.SmoothOut)
    val fill by animateColorAsState(
        when {
            done -> colors.grass
            current -> colors.ink
            else -> colors.surface
        },
        boxSpec,
        label = "step fill",
    )
    val ring by animateColorAsState(
        if (done || current) colors.hairline.copy(alpha = 0f) else colors.hairline,
        boxSpec,
        label = "step ring",
    )
    val numberColor by animateColorAsState(if (current) colors.onInk else colors.muted, boxSpec, label = "step number")
    val diameter by animateDpAsState(
        if (current && !done) currentSize else size,
        if (reduceMotion) snap() else tween(Motion.FAST, easing = Motion.SmoothOut),
        label = "step size",
    )
    val swap by animateFloatAsState(
        if (done) 1f else 0f,
        if (reduceMotion) snap() else tween(Motion.ICON, easing = Motion.Emphasized),
        label = "step number swap",
    )
    val blur = with(LocalDensity.current) { Motion.BlurIcon.toPx() }
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(fill)
            .border(borderWidth, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$number",
            style = if (current && currentSize != size) StepwiseTheme.type.label else StepwiseTheme.type.caption,
            color = numberColor,
            modifier = Modifier.graphicsLayer {
                val scale = 1f - (1f - Motion.ICON_SCALE) * swap
                alpha = 1f - swap
                scaleX = scale
                scaleY = scale
                val r = blur * swap
                renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
            },
        )
        CheckMark(done, colors.ink, Modifier.size(14.dp))
    }
}

/** Segmented step progress: done = green, current = ink, rest = sandstone. Segments fade to their new colour. */
@Composable
fun SegmentedProgress(total: Int, done: Int, modifier: Modifier = Modifier, markCurrent: Boolean = true) {
    if (total <= 0 || !StepwiseTheme.settings.showProgressBars) return
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$done of $total steps done" },
        horizontalArrangement = Arrangement.spacedBy(if (total > 12) 2.dp else 4.dp),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                when {
                    index < done -> colors.grass
                    index == done && markCurrent -> colors.ink
                    else -> colors.recessed
                },
                tween(Motion.FAST, easing = Motion.SmoothOut),
                label = "segment",
            )
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

/** Small 10dp-radius tag chip. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = StepwiseTheme.colors.recessed,
    border: BorderStroke? = null,
    dot: Color? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = StepwiseTheme.type.small, color = StepwiseTheme.colors.ink)
    }
}

/** A choice inside a sheet: icon circle, title, one-line hint, chevron. */
@Composable
fun ChoiceCard(
    icon: ImageVector,
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackground: Color = StepwiseTheme.colors.recessed,
    iconTint: Color = StepwiseTheme.colors.ink,
    border: BorderStroke? = BorderStroke(1.dp, StepwiseTheme.colors.hairline),
    enabled: Boolean = true,
    sound: UiSound = UiSound.Select,
) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .pressClickable(GroupShape, enabled = enabled, sound = sound, onClick = onClick)
            .fillMaxWidth()
            .background(colors.surface)
            .then(if (border != null) Modifier.border(border, GroupShape) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (enabled) iconBackground else colors.canvas),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) iconTint else colors.hairline, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = StepwiseTheme.type.label, color = if (enabled) colors.ink else colors.muted)
            Text(hint, style = StepwiseTheme.type.small, color = colors.muted)
        }
        Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
    }
}

/** White group of settings rows with thin dividers, as in the Settings screens. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(GroupShape).background(StepwiseTheme.colors.surface).padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
fun GroupDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(StepwiseTheme.colors.canvas))
}
