package com.uxankit.stepwise.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.ui.components.CardShape
import com.uxankit.stepwise.ui.components.Chip
import com.uxankit.stepwise.ui.components.DoneBadge
import com.uxankit.stepwise.ui.components.GroupShape
import com.uxankit.stepwise.ui.components.HeaderAction
import com.uxankit.stepwise.ui.components.HeroText
import com.uxankit.stepwise.ui.components.MessageHost
import com.uxankit.stepwise.ui.components.NavHeader
import com.uxankit.stepwise.ui.components.PrimaryCta
import com.uxankit.stepwise.ui.components.ScreenScaffold
import com.uxankit.stepwise.ui.components.SegmentedProgress
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.StepwiseSheet
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.task.EditableStep
import com.uxankit.stepwise.ui.task.StepsEditor
import com.uxankit.stepwise.ui.task.TaskState
import com.uxankit.stepwise.ui.task.withDraft
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** IA 5 · Focus Mode: one step on screen. Done / Skip / Pause / Make it smaller. */
@Composable
fun FocusScreen(
    taskId: String,
    onboarding: Boolean,
    onClose: () -> Unit,
    onOpenTask: () -> Unit,
    onSwitchTask: (String) -> Unit,
    onPaused: () -> Unit,
    onSaveProgress: () -> Unit,
    onFinishOnboarding: (continueFocus: Boolean) -> Unit,
) {
    val user = requireUser()
    val vm = viewModel { FocusViewModel(user.uid, taskId) }
    val state by vm.state.collectAsState()
    var doneInfo by remember { mutableStateOf<StepDoneInfo?>(null) }
    var showSmaller by remember { mutableStateOf(false) }
    val settings = StepwiseTheme.settings
    val haptics = LocalHapticFeedback.current
    val messages = AppGraph.messages

    val task = (state as? TaskState.Ready)?.task
    when {
        state == TaskState.Loading -> Box(Modifier.fillMaxSize().background(StepwiseTheme.colors.canvas), Alignment.Center) {
            CircularProgressIndicator(color = StepwiseTheme.colors.ink)
        }
        state == TaskState.Missing -> LaunchedEffect(Unit) { onClose() }
        doneInfo != null -> StepDoneContent(
            info = doneInfo!!,
            onboarding = onboarding,
            isGuest = user.isGuest,
            nextTaskId = vm.nextTaskId(),
            onNextStep = { doneInfo = null },
            onClose = onClose,
            onSwitchTask = onSwitchTask,
            onSaveProgress = onSaveProgress,
            onFinishOnboarding = onFinishOnboarding,
        )
        task != null -> FocusContent(
            task = task,
            onboarding = onboarding,
            onClose = onClose,
            onOpenTask = onOpenTask,
            onDone = {
                val info = vm.done() ?: return@FocusContent
                if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (settings.showStepDoneScreen || onboarding || info.taskDone) {
                    doneInfo = info
                    messages.showUndo("Step marked done") {
                        vm.undoDone()
                        doneInfo = null
                    }
                } else {
                    messages.showUndo("Step ${info.stepNumber} done") { vm.undoDone() }
                }
            },
            onSmaller = { showSmaller = true },
            onSkip = {
                val original = task
                val next = vm.skip()
                val where = when (original.list) {
                    TaskList.Urgent -> "in Urgent"
                    TaskList.Today -> "in Today"
                    TaskList.Inbox -> "in your Inbox"
                }
                messages.showUndo("Skipped. It’s still $where.") { AppGraph.tasks.save(user.uid, original) }
                if (next != null) onSwitchTask(next) else onClose()
            },
            onPause = {
                vm.pause()
                onPaused()
            },
        )
    }

    if (showSmaller && task != null) {
        MakeSmallerSheet(
            stepNumber = (task.currentStepIndex + 1).coerceAtLeast(1),
            stepTitle = task.currentStep?.title ?: task.title,
            onSave = vm::makeSmaller,
            onDismiss = { showSmaller = false },
        )
    }
}

@Composable
private fun FocusContent(
    task: Task,
    onboarding: Boolean,
    onClose: () -> Unit,
    onOpenTask: () -> Unit,
    onDone: () -> Unit,
    onSmaller: () -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
) {
    val colors = StepwiseTheme.colors
    var menu by remember { mutableStateOf(false) }
    val current = task.currentStep
    ScreenScaffold(
        bottom = {
            MessageHost()
            ServiceCta("Done", onClick = onDone)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundAction(StepIcons.BreakDown, "Make it smaller", onSmaller)
                RoundAction(StepIcons.Skip, "Skip", onSkip)
                RoundAction(StepIcons.Pause, "Pause", onPause)
            }
        },
    ) {
        Box {
            NavHeader(
                overline = "Focus Mode",
                title = task.title,
                leading = HeaderAction(StepIcons.Close, "Close Focus Mode", onClose),
                trailing = HeaderAction(StepIcons.More, "More") { menu = true },
            )
            Box(Modifier.align(Alignment.TopEnd).padding(top = 44.dp)) {
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
                    DropdownMenuItem(text = { Text("Open task page") }, onClick = { menu = false; onOpenTask() })
                }
            }
        }
        StepTrack(task)
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (task.hasSteps && current != null) {
                Text("Step ${task.currentStepIndex + 1} of ${task.steps.size}", style = StepwiseTheme.type.label, color = colors.muted)
                HeroText(current.title)
                task.nextStepAfterCurrent?.let { next ->
                    MetaLine(StepIcons.Flag, "Then: ${next.title}")
                }
            } else {
                Text(
                    when (task.list) {
                        TaskList.Urgent -> "Urgent"
                        TaskList.Today -> "From Today"
                        TaskList.Inbox -> "From your Inbox"
                    },
                    style = StepwiseTheme.type.label,
                    color = colors.muted,
                )
                HeroText(task.title)
                MetaLine(StepIcons.Check, "One step — no breakdown needed")
            }
        }
        if (onboarding) {
            Row(
                Modifier.fillMaxWidth().clip(CircleShape).background(colors.ink).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(StepIcons.Target, contentDescription = null, tint = colors.grass, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "This is Focus Mode. Just this one step — nothing else on screen.",
                    style = StepwiseTheme.type.small,
                    color = colors.onInk,
                )
            }
        }
    }
}

@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    val colors = StepwiseTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = StepwiseTheme.type.body, color = colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The numbered step track under the header. Long lists fall back to a chip. */
@Composable
private fun StepTrack(task: Task) {
    if (!task.hasSteps) return
    val colors = StepwiseTheme.colors
    val currentIndex = task.currentStepIndex
    if (task.steps.size > 7) {
        Chip("${task.stepsDone} of ${task.steps.size} steps done")
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CardShape)
            .background(colors.surface)
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${task.stepsDone} of ${task.steps.size} steps done"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        task.steps.forEachIndexed { index, step ->
            if (index > 0) {
                Box(
                    Modifier.weight(1f).height(2.dp).clip(CircleShape)
                        .background(if (task.steps[index - 1].done) colors.grass else colors.hairline),
                )
            }
            val isCurrent = index == currentIndex
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 36.dp else 28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            step.done -> colors.grass
                            isCurrent -> colors.ink
                            else -> colors.surface
                        },
                    )
                    .then(if (!step.done && !isCurrent) Modifier.border(1.5.dp, colors.hairline, CircleShape) else Modifier)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                if (step.done) {
                    Icon(StepIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        "${index + 1}",
                        style = if (isCurrent) StepwiseTheme.type.label else StepwiseTheme.type.caption,
                        color = if (isCurrent) colors.onInk else colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = StepwiseTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(CardShape).clickable(role = Role.Button, onClick = onClick).padding(4.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(colors.surface).border(1.dp, colors.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = StepwiseTheme.type.caption, color = colors.ink)
    }
}

/** 5.2 Step Done (and 1.5 during onboarding). */
@Composable
private fun StepDoneContent(
    info: StepDoneInfo,
    onboarding: Boolean,
    isGuest: Boolean,
    nextTaskId: String?,
    onNextStep: () -> Unit,
    onClose: () -> Unit,
    onSwitchTask: (String) -> Unit,
    onSaveProgress: () -> Unit,
    onFinishOnboarding: (Boolean) -> Unit,
) {
    val colors = StepwiseTheme.colors
    val title = when {
        !info.taskDone -> "Step ${info.stepNumber} done"
        info.totalSteps > 1 -> "All steps done"
        else -> "Done"
    }
    val subtitle = when {
        info.taskDone -> "That’s the whole thing. Well done."
        info.stepNumber == 1 -> "You started. That’s the hardest part."
        else -> "That’s real progress on “${info.taskTitle}”."
    }
    ScreenScaffold(
        bottom = {
            MessageHost()
            when {
                onboarding && isGuest -> {
                    ServiceCta("Save my progress", onClick = onSaveProgress)
                    PrimaryCta(
                        if (info.taskDone) "Not now" else "Not now — next step",
                        onClick = { onFinishOnboarding(!info.taskDone) },
                        fillWidth = true,
                        tall = true,
                    )
                }
                onboarding -> ServiceCta(if (info.taskDone) "Go to Today" else "Next step", onClick = { onFinishOnboarding(!info.taskDone) })
                !info.taskDone -> {
                    ServiceCta("Next step", onClick = onNextStep)
                    PrimaryCta("Back to Today", onClick = onClose, fillWidth = true, tall = true)
                }
                else -> {
                    ServiceCta("Back to Today", onClick = onClose)
                    if (nextTaskId != null) PrimaryCta("Next task", onClick = { onSwitchTask(nextTaskId) }, fillWidth = true, tall = true)
                }
            }
        },
    ) {
        NavHeader(title = "", leading = HeaderAction(StepIcons.Close, "Close", onClose))
        DoneBadge(Modifier.align(Alignment.CenterHorizontally))
        Text(
            title,
            style = StepwiseTheme.type.hero.copy(fontSize = StepwiseTheme.type.hero.fontSize * 0.8f),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(subtitle, style = StepwiseTheme.type.body, color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        StepCard(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.taskTitle, style = StepwiseTheme.type.label, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${info.stepsDone} of ${info.totalSteps} steps", style = StepwiseTheme.type.small, color = colors.ink)
            }
            SegmentedProgress(total = info.totalSteps, done = info.stepsDone, markCurrent = false)
        }
        if (onboarding && isGuest) {
            StepCard(shape = GroupShape) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(colors.canvas), contentAlignment = Alignment.Center) {
                        Icon(StepIcons.Lock, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Save your progress?", style = StepwiseTheme.type.label, color = colors.ink)
                        Text(
                            "A free account keeps your tasks safe on every device. Optional.",
                            style = StepwiseTheme.type.small,
                            color = colors.muted,
                        )
                    }
                }
            }
        } else if (!info.taskDone && info.nextStepTitle != null) {
            StepCard(shape = GroupShape, spacing = 4) {
                Text("Up next · Step ${info.nextStepNumber}", style = StepwiseTheme.type.small, color = colors.muted)
                Text(info.nextStepTitle, style = StepwiseTheme.type.bodyLargeMedium, color = colors.ink)
            }
        }
    }
}

/** 5.3 "Make it smaller": replaces the current step with tinier ones. */
@Composable
private fun MakeSmallerSheet(stepNumber: Int, stepTitle: String, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var steps by remember { mutableStateOf(emptyList<EditableStep>()) }
    var draft by remember { mutableStateOf("") }
    val all = steps.withDraft(draft)
    StepwiseSheet(onDismiss = onDismiss, title = "Make it smaller") {
        Text(
            "Break “$stepTitle” into tiny actions.",
            style = StepwiseTheme.type.small,
            color = StepwiseTheme.colors.muted,
        )
        StepsEditor(
            steps = steps,
            onChange = { steps = it },
            draft = draft,
            onDraftChange = { draft = it },
            numberPrefix = "$stepNumber.",
            showHint = false,
        )
        ServiceCta(
            "Save and start $stepNumber.1",
            enabled = all.isNotEmpty(),
            onClick = {
                onSave(all.map { it.title })
                onDismiss()
            },
        )
    }
}
