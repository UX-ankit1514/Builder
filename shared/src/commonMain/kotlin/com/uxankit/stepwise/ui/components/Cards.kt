package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

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
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** Round "mark done" toggle used on task and step rows. */
@Composable
fun CheckCircle(checked: Boolean, onToggle: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onToggle)
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
                .background(if (checked) colors.grass else Color.Transparent)
                .border(1.5.dp, if (checked) colors.grass else colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(StepIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
        }
    }
}

/** "Task Row": check circle, title, optional sub-line, chevron. */
@Composable
fun TaskRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    val colors = StepwiseTheme.colors
    val type = StepwiseTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CardShape)
            .background(colors.surface)
            .then(if (border != null) Modifier.border(border, CardShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 4.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = checked, onToggle = onToggle, label = "Mark “$title” done")
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = type.bodyLargeMedium.copy(fontWeight = type.body.fontWeight),
                color = if (checked) colors.muted else colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, style = type.small, color = colors.muted)
        }
        Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
    }
}

/** Segmented step progress: done = green, current = ink, rest = sandstone. */
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
            val color = when {
                index < done -> colors.grass
                index == done && markCurrent -> colors.ink
                else -> colors.recessed
            }
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
) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(colors.surface)
            .then(if (border != null) Modifier.border(border, GroupShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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
