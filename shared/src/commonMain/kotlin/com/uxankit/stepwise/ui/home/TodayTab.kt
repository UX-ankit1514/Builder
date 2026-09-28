package com.uxankit.stepwise.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.SessionUser
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.components.PopNumber
import com.uxankit.stepwise.ui.components.PrimaryCta
import com.uxankit.stepwise.ui.components.SectionHeader
import com.uxankit.stepwise.ui.components.SegmentedProgress
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.StepsIllustration
import com.uxankit.stepwise.ui.components.SwapText
import com.uxankit.stepwise.ui.components.TabHeader
import com.uxankit.stepwise.ui.components.TaskRow
import com.uxankit.stepwise.ui.components.animateRow
import com.uxankit.stepwise.ui.components.pressClickable
import com.uxankit.stepwise.ui.components.reveal
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import com.uxankit.stepwise.util.DateText
import com.uxankit.stepwise.util.Time

/** IA 2 · Today: Urgent (max 3), Continue where you left, Today's tasks (max 5), Done today. */
@Composable
fun TodayTab(
    tasks: List<Task>,
    user: SessionUser,
    onProfile: () -> Unit,
    onStart: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleDone: (Task, Boolean) -> Unit,
    onPickFromInbox: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    val colors = StepwiseTheme.colors
    val today = Time.today()
    val urgent = Planner.urgent(tasks)
    val todays = Planner.today(tasks)
    val continueTask = Planner.continueTask(tasks)
    val doneToday = Planner.doneOn(tasks, today)
    val inboxCount = Planner.inbox(tasks).size
    var showDone by rememberSaveable { mutableStateOf(false) }
    val greeting = DateText.greeting() + (user.firstName?.let { ", $it" } ?: "")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { TabHeader(overline = DateText.long(today), title = greeting, onProfile = onProfile) }

        if (urgent.isEmpty() && todays.isEmpty() && continueTask == null) {
            item(key = "clear-day") { ClearDayCard(inboxCount, onPickFromInbox, onQuickAdd, animateRow()) }
        } else {
            if (urgent.isNotEmpty()) {
                item(key = "urgent-header") {
                    SectionHeader(
                        "Urgent",
                        trailing = "${urgent.size} of ${Limits.URGENT}",
                        icon = StepIcons.Emergency,
                        modifier = animateRow(),
                    )
                }
                item(key = "urgent-" + urgent.first().id) {
                    UrgentCard(
                        urgent.first(),
                        onStart = { onStart(urgent.first().id) },
                        onOpen = { onOpenTask(urgent.first().id) },
                        modifier = animateRow(),
                    )
                }
                items(urgent.drop(1), key = { it.id }) { task ->
                    TaskRow(
                        title = task.title,
                        subtitle = metaLine(task),
                        checked = false,
                        onToggle = { onToggleDone(task, true) },
                        onClick = { onOpenTask(task.id) },
                        border = BorderStroke(1.5.dp, colors.coral),
                        modifier = animateRow(),
                    )
                }
            }
            if (continueTask != null) {
                item(key = "continue") { ContinueCard(continueTask, onContinue = { onStart(continueTask.id) }, modifier = animateRow()) }
            }
            item(key = "today-header") {
                SectionHeader("Today’s tasks", trailing = "${todays.size} of ${Limits.TODAY}", modifier = animateRow())
            }
            if (todays.isEmpty()) {
                item(key = "today-empty") {
                    Text(
                        if (inboxCount > 0) "Nothing else planned. Pick one from your Inbox when you’re ready." else "Nothing else planned.",
                        style = StepwiseTheme.type.body,
                        color = colors.muted,
                        modifier = animateRow().padding(horizontal = 4.dp),
                    )
                }
            }
            items(todays, key = { it.id }) { task ->
                TaskRow(
                    title = task.title,
                    subtitle = task.takeIf { it.hasSteps }?.let { "${it.steps.size} steps · ${it.stepsDone} done" },
                    checked = false,
                    onToggle = { onToggleDone(task, true) },
                    onClick = { onOpenTask(task.id) },
                    modifier = animateRow(),
                )
            }
        }

        if (doneToday.isNotEmpty()) {
            item(key = "done-header") {
                DoneTodayHeader(doneToday.size, expanded = showDone, onToggle = { showDone = !showDone }, modifier = animateRow())
            }
            if (showDone) {
                items(doneToday, key = { "done-" + it.id }) { task ->
                    TaskRow(
                        title = task.title,
                        subtitle = null,
                        checked = true,
                        onToggle = { onToggleDone(task, false) },
                        onClick = { onOpenTask(task.id) },
                        modifier = animateRow(),
                    )
                }
            }
        }
    }
}

internal fun metaLine(task: Task): String? {
    val parts = buildList {
        task.deadline?.let { add(DateText.due(it)) }
        if (task.hasSteps) add("Step ${(task.currentStepIndex + 1).coerceAtLeast(1)} of ${task.steps.size}")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun UrgentCard(task: Task, onStart: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    StepCard(
        modifier = modifier,
        border = BorderStroke(1.5.dp, colors.coral),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
        onClick = onOpen,
    ) {
        Text(task.title, style = StepwiseTheme.type.section, color = colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        metaLine(task)?.let { meta ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(StepIcons.Clock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(meta, style = StepwiseTheme.type.body, color = colors.muted)
            }
        }
        ServiceCta(text = "Start", onClick = onStart)
    }
}

@Composable
private fun ContinueCard(task: Task, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val total = task.steps.size.coerceAtLeast(1)
    val stepNumber = (task.currentStepIndex + 1).coerceAtLeast(1)
    StepCard(modifier = modifier, onClick = onContinue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Continue where you left", style = StepwiseTheme.type.body, color = colors.muted, modifier = Modifier.weight(1f))
            SwapText("Step $stepNumber of $total", style = StepwiseTheme.type.label, color = colors.ink)
        }
        Text(task.title, style = StepwiseTheme.type.bodyLargeMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        SegmentedProgress(total = task.steps.size, done = task.stepsDone)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Next: ${task.currentStep?.title ?: task.title}",
                style = StepwiseTheme.type.body,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PrimaryCta(text = "Continue", onClick = onContinue)
        }
    }
}

/** 2.4 "A clear day". No backlog, no guilt. The illustration and copy rise in one after another. */
@Composable
private fun ClearDayCard(inboxCount: Int, onPickFromInbox: () -> Unit, onQuickAdd: () -> Unit, modifier: Modifier = Modifier) = Column(modifier) {
    val colors = StepwiseTheme.colors
    StepCard(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        spacing = 14,
    ) {
        StepsIllustration(Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally).reveal(0, Motion.MICRO))
        Text(
            "A clear day",
            style = StepwiseTheme.type.title.copy(fontSize = StepwiseTheme.type.section.fontSize * 1.2f),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().reveal(1, Motion.MICRO),
        )
        Text(
            "Nothing is planned yet. Pick up to ${Limits.TODAY} tasks from your Inbox, or tap + to add something new.",
            style = StepwiseTheme.type.body,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().reveal(2, Motion.MICRO),
        )
        if (inboxCount > 0) {
            ServiceCta(text = "Pick from Inbox", onClick = onPickFromInbox, sound = UiSound.Navigate)
        } else {
            ServiceCta(text = "Add a task", onClick = onQuickAdd, sound = UiSound.Open)
        }
    }
    if (inboxCount > 0) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(StepIcons.Inbox, contentDescription = null, tint = colors.muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (inboxCount == 1) "1 task waiting in your Inbox" else "$inboxCount tasks waiting in your Inbox",
                style = StepwiseTheme.type.small,
                color = colors.muted,
            )
        }
    }
}

/** Accordion header: the chevron flips from "v" to "^" and the done rows fade in below it. */
@Composable
private fun DoneTodayHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    val flip by animateFloatAsState(
        if (expanded) -1f else 1f,
        if (StepwiseTheme.settings.reduceMotion) snap() else tween(Motion.FAST, easing = Motion.SmoothOut),
        label = "chevron flip",
    )
    Row(
        modifier = modifier
            .pressClickable(CircleShape, sound = if (expanded) UiSound.Close else UiSound.Open, onClick = onToggle)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(colors.grass), contentAlignment = Alignment.Center) {
            Icon(StepIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("Done today · ", style = StepwiseTheme.type.label, color = colors.ink)
        PopNumber(count, style = StepwiseTheme.type.label, color = colors.ink)
        Spacer(Modifier.weight(1f))
        Icon(
            StepIcons.ChevronDown,
            contentDescription = if (expanded) "Hide done tasks" else "Show done tasks",
            tint = colors.muted,
            modifier = Modifier.size(20.dp).graphicsLayer { scaleY = flip },
        )
    }
}
