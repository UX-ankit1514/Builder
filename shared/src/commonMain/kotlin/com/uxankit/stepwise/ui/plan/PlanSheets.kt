package com.uxankit.stepwise.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.ui.components.ChoiceCard
import com.uxankit.stepwise.ui.components.Chip
import com.uxankit.stepwise.ui.components.GroupShape
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepwiseSheet
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import com.uxankit.stepwise.util.DateText

/** 3.2 "Pick a task — is it urgent?" (IA Flow 3). */
@Composable
fun PlanSheet(
    task: Task,
    all: List<Task>,
    onMove: (TaskList) -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = StepwiseTheme.colors
    val urgent = Planner.count(all, TaskList.Urgent)
    val today = Planner.count(all, TaskList.Today)
    StepwiseSheet(onDismiss = onDismiss, title = task.title) {
        Text("Is it urgent? Pick one — you can change it later.", style = StepwiseTheme.type.small, color = colors.muted)
        ChoiceCard(
            icon = StepIcons.Emergency,
            title = "Yes — mark as urgent",
            hint = "Goes to Urgent · $urgent of ${Limits.URGENT} used",
            onClick = { onMove(TaskList.Urgent) },
            border = BorderStroke(1.5.dp, colors.coral),
        )
        ChoiceCard(
            icon = StepIcons.Sun,
            title = "No — move to Today",
            hint = "Goes to Today’s tasks · $today of ${Limits.TODAY} used",
            onClick = { onMove(TaskList.Today) },
            iconBackground = colors.grass,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(StepIcons.Edit, "Open task page", onOpen, Modifier.weight(1f))
            SoftButton(StepIcons.Trash, "Delete", onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
fun SoftButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(CircleShape)
            .background(colors.canvas)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = StepwiseTheme.type.label, color = colors.ink)
    }
}

/** 3.4 "Too many urgent — gentle check" (IA Flow 7). Also used when Today is full. */
@Composable
fun RebalanceSheet(
    state: RebalanceState,
    all: List<Task>,
    onApply: (Map<String, TaskList>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = StepwiseTheme.colors
    var choices by remember(state) { mutableStateOf(state.choices) }
    val isUrgent = state.target == TaskList.Urgent
    val limit = Planner.limitOf(state.target) ?: Int.MAX_VALUE
    val byId = all.associateBy { it.id }
    val candidateId = state.taskIds.last()

    val othersIn = { list: TaskList -> Planner.open(all).count { it.list == list && it.id !in choices } }
    val urgentCount = othersIn(TaskList.Urgent) + choices.values.count { it == TaskList.Urgent }
    val todayCount = othersIn(TaskList.Today) + choices.values.count { it == TaskList.Today }
    val targetCount = if (isUrgent) urgentCount else todayCount
    val options = if (isUrgent) TaskList.entries.reversed() else listOf(TaskList.Today, TaskList.Inbox)

    StepwiseSheet(
        onDismiss = onDismiss,
        title = if (isUrgent) "Which ones are truly urgent?" else "What can wait?",
    ) {
        Text(
            if (isUrgent) "Urgent works best with ${Limits.URGENT} or fewer. Move one back — nothing is deleted."
            else "Today works best with ${Limits.TODAY} or fewer. Move one back — nothing is deleted.",
            style = StepwiseTheme.type.small,
            color = colors.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isUrgent) {
                Chip(
                    "Urgent · $urgentCount of ${Limits.URGENT}",
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.coral),
                    dot = colors.coral,
                )
            }
            Chip("Today · $todayCount of ${Limits.TODAY}" + if (todayCount >= Limits.TODAY) " (full)" else "")
        }
        state.taskIds.forEach { id ->
            val task = byId[id] ?: return@forEach
            Column(
                Modifier.fillMaxWidth().clip(GroupShape).background(colors.canvas).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text(task.title, style = StepwiseTheme.type.label, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val hint = when {
                        id == candidateId -> if (isUrgent) "Just marked urgent" else "Just added to Today"
                        task.deadline != null -> DateText.due(task.deadline)
                        else -> DateText.added(task.createdAt)
                    }
                    Text(hint, style = StepwiseTheme.type.small, color = colors.muted)
                }
                Segmented(
                    options = options,
                    selected = choices.getValue(id),
                    onSelect = { choices = choices + (id to it) },
                )
            }
        }
        if (isUrgent && todayCount > Limits.TODAY) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(StepIcons.Inbox, contentDescription = null, tint = colors.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Today is full, so Inbox is suggested.", style = StepwiseTheme.type.small, color = colors.muted)
            }
        }
        Spacer(Modifier.height(4.dp))
        ServiceCta(
            text = if (isUrgent) "Keep these $targetCount urgent" else "Keep these $targetCount for today",
            onClick = { onApply(choices) },
            enabled = targetCount <= limit,
        )
    }
}

@Composable
private fun Segmented(options: List<TaskList>, selected: TaskList, onSelect: (TaskList) -> Unit) {
    val colors = StepwiseTheme.colors
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(colors.surface).padding(4.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = option.name,
                style = StepwiseTheme.type.small,
                color = if (isSelected) Color.White else colors.ink,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.ink else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 9.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
