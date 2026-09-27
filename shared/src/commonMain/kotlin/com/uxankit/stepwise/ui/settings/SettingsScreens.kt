package com.uxankit.stepwise.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.platform.LocalPlatform
import com.uxankit.stepwise.ui.account.AccountViewModel
import com.uxankit.stepwise.ui.components.Avatar
import com.uxankit.stepwise.ui.components.ErrorNote
import com.uxankit.stepwise.ui.components.GroupDivider
import com.uxankit.stepwise.ui.components.GroupLabel
import com.uxankit.stepwise.ui.components.HeaderAction
import com.uxankit.stepwise.ui.components.MessageHost
import com.uxankit.stepwise.ui.components.NavHeader
import com.uxankit.stepwise.ui.components.PrimaryCta
import com.uxankit.stepwise.ui.components.ScreenScaffold
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.SettingsGroup
import com.uxankit.stepwise.ui.components.SettingsRow
import com.uxankit.stepwise.ui.components.SettingsToggleRow
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.StepwiseSheet
import com.uxankit.stepwise.ui.components.TextLink
import com.uxankit.stepwise.ui.rememberGoogleSignIn
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** 8.1 Settings. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccessibility: () -> Unit,
    onAccount: (openDelete: Boolean) -> Unit,
    onSignIn: () -> Unit,
) {
    val user = requireUser()
    val vm = viewModel { SettingsViewModel(user.uid) }
    val accountVm = viewModel { AccountViewModel() }
    val settings by vm.settings.collectAsState()
    val platform = LocalPlatform.current
    val colors = StepwiseTheme.colors

    ScreenScaffold(bodySpacing = 12, bottom = { MessageHost() }) {
        NavHeader(title = "Settings", leading = HeaderAction(StepIcons.ChevronLeft, "Back", onBack))

        StepCard(
            onClick = if (user.isGuest) onSignIn else ({ onAccount(false) }),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user.name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (user.isGuest) "Guest" else user.name ?: "Your account", style = StepwiseTheme.type.label, color = colors.ink)
                    Text(
                        if (user.isGuest) "Not backed up yet · Sign in with Google" else "Synced · ${user.email.orEmpty()}",
                        style = StepwiseTheme.type.small,
                        color = colors.muted,
                    )
                }
                Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
            }
        }

        GroupLabel("Your day")
        SettingsGroup {
            SettingsRow(title = "Reminders", icon = StepIcons.Bell, value = "Coming soon", enabled = false)
            GroupDivider()
            SettingsToggleRow(
                title = "AI step suggestions",
                subtitle = "Coming soon. Only when you ask — never saved without you.",
                icon = StepIcons.Sparkles,
                checked = settings.aiSuggestions,
                onCheckedChange = { on -> vm.update { it.copy(aiSuggestions = on) } },
            )
        }

        GroupLabel("Comfort")
        SettingsGroup {
            SettingsRow(title = "Accessibility", icon = StepIcons.Eye, onClick = onAccessibility)
        }

        GroupLabel("Your data")
        SettingsGroup {
            if (user.isGuest) {
                SettingsRow(title = "Sign in with Google", icon = StepIcons.User, subtitle = "Keep your tasks safe on every device", onClick = onSignIn)
            } else {
                SettingsRow(title = "Account", icon = StepIcons.User, onClick = { onAccount(false) })
            }
            GroupDivider()
            SettingsRow(
                title = if (vm.exporting) "Preparing your file…" else "Export data",
                icon = StepIcons.Download,
                enabled = !vm.exporting,
                onClick = { vm.export(platform.fileSharer) },
            )
            if (!user.isGuest) {
                GroupDivider()
                SettingsRow(
                    title = "Log out",
                    icon = StepIcons.Logout,
                    enabled = !accountVm.busy,
                    showChevron = false,
                    onClick = { accountVm.signOut { platform.googleSignIn.signOut() } },
                )
            }
            GroupDivider()
            SettingsRow(
                title = if (user.isGuest) "Delete my data" else "Delete account",
                icon = StepIcons.Trash,
                onClick = { onAccount(true) },
            )
        }
        ErrorNote(accountVm.error)
    }
}

/** 8.3 Accessibility. */
@Composable
fun AccessibilityScreen(onBack: () -> Unit) {
    val user = requireUser()
    val vm = viewModel { SettingsViewModel(user.uid) }
    val settings by vm.settings.collectAsState()

    ScreenScaffold(bodySpacing = 12) {
        NavHeader(title = "Accessibility", leading = HeaderAction(StepIcons.ChevronLeft, "Back", onBack))
        SettingsGroup {
            SettingsRow(title = "Text size", value = "Follows your phone")
            GroupDivider()
            SettingsToggleRow(
                title = "Reduce motion",
                subtitle = "Calmer transitions, no celebrations moving",
                checked = settings.reduceMotion,
                onCheckedChange = { on -> vm.update { it.copy(reduceMotion = on) } },
            )
        }
        SettingsGroup {
            SettingsToggleRow(
                title = "Show progress bars",
                subtitle = "Hide them if they feel like pressure",
                checked = settings.showProgressBars,
                onCheckedChange = { on -> vm.update { it.copy(showProgressBars = on) } },
            )
            GroupDivider()
            SettingsToggleRow(
                title = "Step Done screen",
                subtitle = "Show a short “step done” moment",
                checked = settings.showStepDoneScreen,
                onCheckedChange = { on -> vm.update { it.copy(showStepDoneScreen = on) } },
            )
        }
        SettingsGroup {
            SettingsToggleRow(
                title = "Haptics",
                checked = settings.haptics,
                onCheckedChange = { on -> vm.update { it.copy(haptics = on) } },
            )
            GroupDivider()
            SettingsToggleRow(
                title = "Higher contrast",
                checked = settings.higherContrast,
                onCheckedChange = { on -> vm.update { it.copy(higherContrast = on) } },
            )
        }
    }
}

/** 8.4 Account & export data, 8.5 Delete account. */
@Composable
fun AccountScreen(openDelete: Boolean, onBack: () -> Unit, onSignIn: () -> Unit) {
    val user = requireUser()
    val settingsVm = viewModel { SettingsViewModel(user.uid) }
    val accountVm = viewModel { AccountViewModel() }
    val platform = LocalPlatform.current
    val colors = StepwiseTheme.colors
    var confirmDelete by remember { mutableStateOf(openDelete) }

    val reauthAndDelete = rememberGoogleSignIn(onError = accountVm::showError) { tokens ->
        accountVm.deleteAccount(tokens) { platform.googleSignIn.signOut() }
    }

    ScreenScaffold(bodySpacing = 12, bottom = { MessageHost() }) {
        NavHeader(title = "Account", leading = HeaderAction(StepIcons.ChevronLeft, "Back", onBack))
        SettingsGroup {
            SettingsRow(title = "Email", icon = StepIcons.Mail, value = if (user.isGuest) "Not signed in" else user.email.orEmpty())
            GroupDivider()
            SettingsRow(
                title = "Sync",
                icon = StepIcons.Download,
                value = if (user.isGuest) "This device only" else "Automatic",
            )
        }
        if (user.isGuest) {
            PrimaryCta("Sign in with Google", onClick = onSignIn, fillWidth = true, tall = true)
        }

        GroupLabel("Export data")
        StepCard(spacing = 12) {
            Text("Get a copy of all your tasks, steps and notes.", style = StepwiseTheme.type.bodyLargeMedium, color = colors.ink)
            Text("You’ll get a JSON file you can save or send anywhere.", style = StepwiseTheme.type.small, color = colors.muted)
            PrimaryCta(
                "Export my data",
                onClick = { settingsVm.export(platform.fileSharer) },
                fillWidth = true,
                busy = settingsVm.exporting,
            )
        }

        ErrorNote(accountVm.error)
        if (!user.isGuest) {
            PrimaryCta(
                "Log out",
                onClick = { accountVm.signOut { platform.googleSignIn.signOut() } },
                fillWidth = true,
                tall = true,
                busy = accountVm.busy && !confirmDelete,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(StepIcons.Trash, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
            TextLink(
                if (user.isGuest) "Delete my data" else "Delete account",
                onClick = { confirmDelete = true },
                color = colors.ink,
            )
        }
    }

    if (confirmDelete) {
        StepwiseSheet(onDismiss = { confirmDelete = false }, title = if (user.isGuest) "Delete your data?" else "Delete your account?") {
            Text(
                "This permanently removes your tasks, steps and notes from every device. It can’t be undone." +
                    if (user.isGuest) "" else " You’ll confirm with Google first.",
                style = StepwiseTheme.type.small,
                color = colors.muted,
            )
            PrimaryCta("Export my data first", onClick = { settingsVm.export(platform.fileSharer) }, fillWidth = true, tall = true, busy = settingsVm.exporting)
            ErrorNote(accountVm.error)
            ServiceCta(
                if (user.isGuest) "Delete my data" else "Delete account",
                busy = accountVm.busy,
                onClick = {
                    if (user.isGuest) accountVm.deleteAccount(null) { } else reauthAndDelete()
                },
            )
            TextLink("Keep my account", onClick = { confirmDelete = false }, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
