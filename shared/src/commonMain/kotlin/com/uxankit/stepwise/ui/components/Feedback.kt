package com.uxankit.stepwise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.AppMessage
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

    val reduceMotion = StepwiseTheme.settings.reduceMotion
    AnimatedVisibility(
        visible = message != null,
        enter = if (reduceMotion) fadeIn() else fadeIn() + slideInVertically { it / 2 },
        exit = if (reduceMotion) fadeOut() else fadeOut() + slideOutVertically { it / 2 },
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
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { act(message.link) }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        if (message.actionLabel != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { act(message.action) }
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

/** Inline, calm error text. Never red alarm banners. */
@Composable
fun ErrorNote(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    Text(
        text,
        style = StepwiseTheme.type.small,
        color = StepwiseTheme.colors.ink,
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(StepwiseTheme.colors.recessed)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
