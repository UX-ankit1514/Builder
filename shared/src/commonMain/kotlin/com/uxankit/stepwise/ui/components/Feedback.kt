package com.uxankit.stepwise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.AppMessage
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.sound.rememberUiSounds
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.delay

/** Shows the current app message (with Undo) wherever a screen places it. */
@Composable
fun MessageHost(modifier: Modifier = Modifier) {
    val message by AppGraph.messages.current.collectAsState()
    var shown by remember { mutableStateOf<AppMessage?>(null) }
    if (message != null) shown = message

    LaunchedEffect(message?.id) {
        val id = message?.id ?: return@LaunchedEffect
        delay(if (message?.action != null) 6_000 else 4_000)
        AppGraph.messages.dismiss(id)
    }

    // Toast: rises 16dp with a slight scale on the slower open clock, leaves on the faster close clock.
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val rise = with(LocalDensity.current) { Motion.DistanceToast.roundToPx() }
    AnimatedVisibility(
        visible = message != null,
        enter = if (reduceMotion) {
            fadeIn(tween(Motion.QUICK))
        } else {
            fadeIn(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) +
                slideInVertically(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) { rise } +
                scaleIn(tween(Motion.MEDIUM, easing = Motion.SmoothOut), initialScale = Motion.TOAST_SCALE)
        },
        exit = if (reduceMotion) {
            fadeOut(tween(Motion.QUICK))
        } else {
            fadeOut(tween(Motion.FAST, easing = Motion.SmoothOut)) +
                slideOutVertically(tween(Motion.FAST, easing = Motion.SmoothOut)) { rise } +
                scaleOut(tween(Motion.FAST, easing = Motion.SmoothOut), targetScale = Motion.TOAST_SCALE)
        },
        modifier = modifier,
    ) {
        shown?.let { UndoBar(it) }
    }
}

@Composable
fun UndoBar(message: AppMessage, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    fun act(block: (() -> Unit)?) {
        block?.invoke()
        AppGraph.messages.dismiss(message.id)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(colors.ink)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 18.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(message.text, style = StepwiseTheme.type.small, color = Color.White, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        if (message.linkLabel != null) {
            Text(
                message.linkLabel,
                style = StepwiseTheme.type.small.copy(textDecoration = TextDecoration.Underline),
                color = Color.White,
                modifier = Modifier
                    .pressClickable(CircleShape, sound = UiSound.Select) { act(message.link) }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        if (message.actionLabel != null) {
            Row(
                modifier = Modifier
                    .pressClickable(CircleShape, sound = UiSound.Unsave) { act(message.action) }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(StepIcons.Undo, contentDescription = null, tint = colors.grass, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(message.actionLabel, style = StepwiseTheme.type.caption, color = colors.grass)
            }
        }
    }
}

/** Inline, calm error text. Never red alarm banners. It opens and closes softly, never shakes. */
@Composable
fun ErrorNote(text: String?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(text) }
    if (text != null) shown = text
    val sounds = rememberUiSounds()
    LaunchedEffect(text) { if (text != null) sounds.play(UiSound.Error) }
    val visibility = remember { MutableTransitionState(text != null) }
    visibility.targetState = text != null
    // Once it has fully closed, leave no empty slot behind for the parent's spacing.
    if (!visibility.currentState && !visibility.targetState) return

    val reduceMotion = StepwiseTheme.settings.reduceMotion
    AnimatedVisibility(
        visibleState = visibility,
        enter = if (reduceMotion) {
            fadeIn(tween(Motion.QUICK))
        } else {
            fadeIn(tween(Motion.FAST, easing = Motion.SmoothOut)) + expandVertically(tween(Motion.FAST, easing = Motion.SmoothOut))
        },
        exit = if (reduceMotion) {
            fadeOut(tween(Motion.QUICK))
        } else {
            fadeOut(tween(Motion.QUICK, easing = Motion.SmoothOut)) + shrinkVertically(tween(Motion.FAST, easing = Motion.SmoothOut))
        },
        modifier = modifier,
    ) {
        Text(
            shown.orEmpty(),
            style = StepwiseTheme.type.small,
            color = StepwiseTheme.colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(StepwiseTheme.colors.recessed)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
