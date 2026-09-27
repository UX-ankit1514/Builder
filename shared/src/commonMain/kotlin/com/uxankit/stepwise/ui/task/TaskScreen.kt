package com.uxankit.stepwise.ui.task

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.ui.components.ChoiceCard
import com.uxankit.stepwise.ui.components.Chip
import com.uxankit.stepwise.ui.components.CircleIconButton
import com.uxankit.stepwise.ui.components.GroupDivider
import com.uxankit.stepwise.ui.components.GroupShape
import com.uxankit.stepwise.ui.components.HeaderAction
import com.uxankit.stepwise.ui.components.MessageHost
import com.uxankit.stepwise.ui.components.NavHeader
import com.uxankit.stepwise.ui.components.PrimaryCta
import com.uxankit.stepwise.ui.components.ScreenScaffold
import com.uxankit.stepwise.ui.components.SectionHeader
import com.uxankit.stepwise.ui.components.SegmentedProgress
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.StepsIllustration
import com.uxankit.stepwise.ui.components.StepwiseSheet
import com.uxankit.stepwise.ui.components.StepwiseTextField
import com.uxankit.stepwise.ui.components.TextLink
import com.uxankit.stepwise.ui.plan.RebalanceSheet
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import com.uxankit.stepwise.util.DateText
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** IA 6 · Task Page: title, notes, deadline, steps list + progress bar, Break Down. */
@Composable
fun TaskScreen(
    taskId: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onBreakDown: () -> Unit,
) {
    val user = requireUser()
    val vm = viewModel { TaskViewModel(user.uid, taskId) }
    val state by vm.state.collectAsState()
    val all by vm.all.collectAsState()
    val rebalance by vm.planning.rebalance.collectAsState()

    when (val s = state) {
        TaskState.Loading -> Box(Modifier.fillMaxSize().background(StepwiseTheme.colors.canvas), Alignment.Center) {
            CircularProgressIndicator(color = StepwiseTheme.colors.ink)
        }
        TaskState.Missing -> LaunchedEffect(Unit) { onBack() }
        is TaskState.Ready -> TaskContent(
            task = s.task,
            vm = vm,
            onBack = onBack,
            onStart = onStart,
            onBreakDown = onBreakDown,
        )
    }

    rebalance?.let {
        RebalanceSheet(state = it, all = all.orEmpty(), onApply = vm::applyRebalance, onDismiss = vm.planning::dismissRebalance)
    }
}

@Composable
private fun TaskContent(
    task: Task,
    vm: TaskViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onBreakDown: () -> Unit,
) {
    val colors = StepwiseTheme.colors
    var menu by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var chooseHow by remember { mutableStateOf(false) }

    ScreenScaffold(
        bodySpacing = 16,
        bottom = {
            MessageHost()
            when {
                task.isDone -> PrimaryCta("Mark as not done", onClick = { vm.setDone(false) }, fillWidth = true, tall = true)
                task.hasSteps -> ServiceCta("Start step ${(task.currentStepIndex + 1).coerceAtLeast(1)}", onClick = onStart)
                else -> ServiceCta("Start", onClick = onStart)
            }
        },
    ) {
        Box {
            NavHeader(
                title = "Task",
                leading = HeaderAction(StepIcons.ChevronLeft, "Back", onBack),
                trailing = HeaderAction(StepIcons.More, "Task options") { menu = true },
            )
            Box(Modifier.align(Alignment.TopEnd).padding(top = 44.dp)) {
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
                    val moves = listOf(
                        TaskList.Urgent to "Mark as urgent",
                        TaskList.Today to "Move to Today",
                        TaskList.Inbox to "Move to Inbox",
                    ).filter { (list, _) -> list != task.list }
                    moves.forEach { (list, label) ->
                        MenuItem(label) { menu = false; vm.move(list) }
                    }
                    MenuItem("Rename") { menu = false; editTitle = true }
                    if (!task.isDone) MenuItem("Mark as done") { menu = false; vm.setDone(true) }
                    MenuItem("Delete task") {
                        menu = false
                        vm.delete()
                        onBack()
                    }
                }
            }
        }

        Text(
            task.title,
            style = StepwiseTheme.type.title,
            color = colors.ink,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clickable(role = Role.Button, onClickLabel = "Rename") { editTitle = true },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Chip(
                when (task.list) {
                    TaskList.Urgent -> "Urgent"
                    TaskList.Today -> "Today"
                    TaskList.Inbox -> "Inbox"
                },
                dot = if (task.list == TaskList.Urgent) colors.coral else null,
            )
            task.deadline?.let { Chip("Due ${DateText.short(it)}") }
            if (task.isDone) Chip("Done", color = colors.grass)
        }

        Column(Modifier.fillMaxWidth().clip(GroupShape).background(colors.surface)) {
            DetailRow(
                icon = StepIcons.Calendar,
                label = "Deadline",
                value = task.deadline?.let(DateText::long) ?: "Add a deadline (optional)",
                onClick = { pickDate = true },
            )
            GroupDivider()
            DetailRow(
                icon = StepIcons.Edit,
                label = "Notes",
                value = task.notes.ifBlank { "Add notes (optional)" },
                onClick = { editNotes = true },
            )
        }

        if (task.hasSteps) {
            StepsCard(task, onToggle = vm::toggleStep, onEdit = onBreakDown)
        } else {
            SectionHeader("Steps", trailing = "None yet")
            StepCard(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
                spacing = 12,
            ) {
                StepsIllustration(Modifier.fillMaxWidth(0.42f).align(Alignment.CenterHorizontally))
                Text(
                    "Big tasks feel lighter as small steps",
                    style = StepwiseTheme.type.bodyLargeMedium,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Split it into a few actions you could start right now.",
                    style = StepwiseTheme.type.small,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ServiceCta("Break it down", onClick = { chooseHow = true })
            }
            TextLink("Or start it as one task", onClick = onStart, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    if (chooseHow) {
        BreakDownHowSheet(
            onMyself = {
                chooseHow = false
                onBreakDown()
            },
            onDismiss = { chooseHow = false },
        )
    }
    if (editTitle) {
        TextEditSheet(
            title = "Rename task",
            initial = task.title,
            maxLength = Limits.TITLE_MAX,
            singleLine = true,
            onSave = vm::rename,
            onDismiss = { editTitle = false },
        )
    }
    if (editNotes) {
        TextEditSheet(
            title = "Notes",
            initial = task.notes,
            maxLength = Limits.NOTES_MAX,
            singleLine = false,
            onSave = vm::setNotes,
            onDismiss = { editNotes = false },
        )
    }
    if (pickDate) {
        DeadlinePicker(
            initial = task.deadline,
            onPick = vm::setDeadline,
            onDismiss = { pickDate = false },
        )
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, style = StepwiseTheme.type.body, color = StepwiseTheme.colors.ink) }, onClick = onClick)
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val colors = StepwiseTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = StepwiseTheme.type.small, color = colors.muted)
            Text(value, style = StepwiseTheme.type.body, color = colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Icon(StepIcons.ChevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
    }
}

/** 4.6 "Steps saved": numbered steps with a progress bar. Tap a number to tick it. */
@Composable
private fun StepsCard(task: Task, onToggle: (String) -> Unit, onEdit: () -> Unit) {
    val colors = StepwiseTheme.colors
    StepCard(shape = GroupShape, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), spacing = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Steps", style = StepwiseTheme.type.section, color = colors.ink, modifier = Modifier.weight(1f))
            Text("${task.stepsDone} of ${task.steps.size} done", style = StepwiseTheme.type.small, color = colors.muted)
        }
        SegmentedProgress(total = task.steps.size, done = task.stepsDone, markCurrent = false)
        task.steps.forEachIndexed { index, step ->
            val isCurrent = index == task.currentStepIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Checkbox) { onToggle(step.id) }
                        .semantics {
                            contentDescription = "Step ${index + 1}"
                            stateDescription = if (step.done) "Done" else "Not done"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    step.done -> colors.grass
                                    isCurrent -> colors.ink
                                    else -> colors.surface
                                },
                            )
                            .then(if (!step.done && !isCurrent) Modifier.border(BorderStroke(1.dp, colors.hairline), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (step.done) {
                            Icon(StepIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
                        } else {
                            Text("${index + 1}", style = StepwiseTheme.type.caption, color = if (isCurrent) colors.onInk else colors.muted)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Step ${index + 1}", style = StepwiseTheme.type.small, color = colors.muted)
                    Text(step.title, style = StepwiseTheme.type.body, color = if (step.done) colors.muted else colors.ink)
                }
            }
        }
        TextLink("Edit steps", onClick = onEdit, color = colors.ink)
    }
}

/** 4.2 "How do you want to break it down?" AI suggestions come in the next milestone. */
@Composable
private fun BreakDownHowSheet(onMyself: () -> Unit, onDismiss: () -> Unit) {
    val colors = StepwiseTheme.colors
    StepwiseSheet(onDismiss = onDismiss, title = "How do you want to break it down?") {
        ChoiceCard(
            icon = StepIcons.Edit,
            title = "Add steps myself",
            hint = "Type a few small actions, in order.",
            onClick = onMyself,
            border = BorderStroke(1.5.dp, colors.ink),
        )
        ChoiceCard(
            icon = StepIcons.Sparkles,
            title = "AI suggests steps",
            hint = "Coming soon. You’ll edit or accept them — nothing is saved until you say so.",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
fun TextEditSheet(
    title: String,
    initial: String,
    maxLength: Int,
    singleLine: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    fun save() {
        onSave(value)
        onDismiss()
    }
    StepwiseSheet(onDismiss = onDismiss, title = title) {
        StepwiseTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = if (singleLine) "Task name" else "Anything that helps you start",
            singleLine = singleLine,
            maxLength = maxLength,
            onImeAction = { if (singleLine) save() },
        )
        ServiceCta("Save", onClick = ::save, enabled = !singleLine || value.isNotBlank())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(initial: LocalDate?, onPick: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    val colors = StepwiseTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
    )
    val pickerColors = DatePickerDefaults.colors(
        containerColor = colors.surface,
        selectedDayContainerColor = colors.ink,
        selectedDayContentColor = colors.onInk,
        todayDateBorderColor = colors.grass,
        todayContentColor = colors.ink,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                }
                onDismiss()
            }) { Text("Save", color = colors.ink) }
        },
        dismissButton = {
            TextButton(onClick = {
                if (initial != null) onPick(null)
                onDismiss()
            }) { Text(if (initial != null) "Remove deadline" else "Cancel", color = colors.muted) }
        },
    ) {
        DatePicker(state = state, colors = pickerColors, showModeToggle = false)
    }
}
