package com.uxankit.stepwise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

enum class HomeTab(val label: String, val icon: ImageVector) {
    Today("Today", StepIcons.Sun),
    Inbox("Inbox", StepIcons.Inbox),
    Progress("Progress", StepIcons.Progress),
}

private val TabHeight = 44.dp

/** Floating white pill with the three tabs, plus the dark Quick Capture button (IA: visible on every tab). */
@Composable
fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val reduceMotion = StepwiseTheme.settings.reduceMotion

    // Tabs sliding: the green pill follows the selected tab's measured left edge and width.
    val tabBounds = remember { mutableStateMapOf<HomeTab, Pair<Float, Float>>() }
    val target = tabBounds[selected]
    var placed by remember { mutableStateOf(false) }
    val pillSpec: AnimationSpec<Float> = if (placed && !reduceMotion) Motion.slideSpring() else snap()
    val pillX = animateFloatAsState(target?.first ?: 0f, pillSpec, label = "pill x")
    val pillWidth = animateFloatAsState(target?.second ?: 0f, pillSpec, label = "pill width")
    SideEffect { if (target != null) placed = true }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.hairline, CircleShape)
                .padding(6.dp)
                .drawBehind {
                    val height = TabHeight.toPx()
                    drawRoundRect(
                        color = colors.grass,
                        topLeft = Offset(pillX.value, (size.height - height) / 2),
                        size = Size(pillWidth.value, height),
                        cornerRadius = CornerRadius(height / 2),
                    )
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.entries.forEach { tab ->
                TabButton(
                    tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.onPlaced { tabBounds[tab] = it.positionInParent().x to it.size.width.toFloat() },
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(58.dp)
                .pressClickable(CircleShape, sound = UiSound.Open, onClick = onAdd)
                .background(colors.ink)
                .semantics { contentDescription = "Quick add a task" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(StepIcons.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun TabButton(tab: HomeTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val start by animateDpAsState(
        if (selected) 12.dp else 10.dp,
        if (reduceMotion) snap() else tween(Motion.FAST, easing = Motion.SmoothOut),
        label = "tab padding",
    )
    Row(
        modifier = modifier
            .height(TabHeight)
            .pressClickable(CircleShape, role = Role.Tab, sound = UiSound.Navigate, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = tab.label
            }
            .padding(start = start, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = selected,
            enter = if (reduceMotion) {
                EnterTransition.None
            } else {
                fadeIn(tween(Motion.FAST, easing = Motion.SmoothOut)) +
                    expandHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut), expandFrom = Alignment.Start)
            },
            exit = if (reduceMotion) {
                ExitTransition.None
            } else {
                fadeOut(tween(Motion.QUICK, easing = Motion.SmoothOut)) +
                    shrinkHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut), shrinkTowards = Alignment.Start)
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(tab.label, style = StepwiseTheme.type.label, color = colors.ink, maxLines = 1)
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}
