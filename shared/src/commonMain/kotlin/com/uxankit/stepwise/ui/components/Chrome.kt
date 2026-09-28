package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.sound.rememberUiSounds
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** Top of the Today / Inbox / Progress tabs: small overline, big title, profile button. */
@Composable
fun TabHeader(overline: String, title: String, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(overline, style = StepwiseTheme.type.body, color = colors.muted)
            Text(title, style = StepwiseTheme.type.title, color = colors.ink, modifier = Modifier.semantics { heading() })
        }
        CircleIconButton(StepIcons.User, contentDescription = "Settings", onClick = onProfile, sound = UiSound.Navigate)
    }
}

/** A circle button in a [NavHeader]. Back navigates; close and "more" pass their own [sound]. */
data class HeaderAction(
    val icon: ImageVector,
    val label: String,
    val sound: UiSound = UiSound.Navigate,
    val onClick: () -> Unit,
)

/** Centered title with optional circle buttons on both sides (Task, Focus Mode, Settings...). */
@Composable
fun NavHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    leading: HeaderAction? = null,
    trailing: HeaderAction? = null,
) {
    val colors = StepwiseTheme.colors
    Row(modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp)) {
            if (leading != null) CircleIconButton(leading.icon, leading.label, leading.onClick, sound = leading.sound)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (overline != null) Text(overline, style = StepwiseTheme.type.caption, color = colors.muted)
            Text(
                title,
                style = if (overline != null) StepwiseTheme.type.bodyLargeMedium else StepwiseTheme.type.label,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box(Modifier.size(44.dp)) {
            if (trailing != null) CircleIconButton(trailing.icon, trailing.label, trailing.onClick, sound = trailing.sound)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = StepwiseTheme.colors.coral,
) {
    val colors = StepwiseTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = StepwiseTheme.type.section, color = colors.ink, modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) SwapText(trailing, style = StepwiseTheme.type.body, color = colors.muted)
    }
}

/** Small grey label above a settings group ("Your day", "Comfort"...). */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = StepwiseTheme.type.small,
        color = StepwiseTheme.colors.muted,
        modifier = modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() },
    )
}

/** Brand Logo: green rounded square with the "S" mark and the wordmark. */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(StepwiseTheme.colors.grass),
            contentAlignment = Alignment.Center,
        ) {
            Text("S", style = StepwiseTheme.type.section, color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text("Stepwise", style = StepwiseTheme.type.bodyLargeMedium, color = StepwiseTheme.colors.ink)
    }
}

/** Onboarding progress: the current page is a short ink bar, the rest are dots. */
@Composable
fun PageDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (i == index) 20.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == index) StepwiseTheme.colors.ink else StepwiseTheme.colors.hairline),
            )
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    sound: UiSound = UiSound.Navigate,
    onClick: (() -> Unit)? = null,
) {
    val colors = StepwiseTheme.colors
    val sounds = rememberUiSounds()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(role = Role.Button) {
                        sounds.play(sound)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) colors.ink else colors.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = StepwiseTheme.type.body, color = if (enabled) colors.ink else colors.muted)
            if (subtitle != null) Text(subtitle, style = StepwiseTheme.type.small, color = colors.muted)
        }
        if (value != null) {
            Text(value, style = StepwiseTheme.type.small, color = colors.muted)
            Spacer(Modifier.width(6.dp))
        }
        if (onClick != null && showChevron) {
            Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
) {
    val colors = StepwiseTheme.colors
    val sounds = rememberUiSounds()
    val toggle = { on: Boolean ->
        sounds.play(if (on) UiSound.ToggleOn else UiSound.ToggleOff)
        onCheckedChange(on)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { toggle(!checked) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = StepwiseTheme.type.body, color = colors.ink)
            if (subtitle != null) Text(subtitle, style = StepwiseTheme.type.small, color = colors.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = toggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.grass,
                checkedBorderColor = colors.grass,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.recessed,
                uncheckedBorderColor = colors.recessed,
            ),
        )
    }
}

/** Initial in a green circle, for the account card. */
@Composable
fun Avatar(name: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(StepwiseTheme.colors.grass).border(0.dp, Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name?.trim()?.firstOrNull()?.uppercase()
        if (initial != null) {
            Text(initial, style = StepwiseTheme.type.label, color = StepwiseTheme.colors.ink)
        } else {
            Icon(StepIcons.User, contentDescription = null, tint = StepwiseTheme.colors.ink, modifier = Modifier.size(20.dp))
        }
    }
}
