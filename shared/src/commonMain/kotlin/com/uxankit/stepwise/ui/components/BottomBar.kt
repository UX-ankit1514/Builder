package com.uxankit.stepwise.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

enum class HomeTab(val label: String, val icon: ImageVector) {
    Today("Today", StepIcons.Sun),
    Inbox("Inbox", StepIcons.Inbox),
    Progress("Progress", StepIcons.Progress),
}

/** Floating white pill with the three tabs, plus the dark Quick Capture button (IA: visible on every tab). */
@Composable
fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
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
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.entries.forEach { tab ->
                TabButton(tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(colors.ink)
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = "Quick add a task" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(StepIcons.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun TabButton(tab: HomeTab, selected: Boolean, onClick: () -> Unit) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (selected) colors.grass else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = tab.label
            }
            .animateContentSize()
            .padding(start = if (selected) 12.dp else 10.dp, end = if (selected) 16.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(tab.label, style = StepwiseTheme.type.label, color = colors.ink)
        }
    }
}
