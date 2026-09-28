package com.uxankit.stepwise.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.TabHeader
import com.uxankit.stepwise.ui.components.TaskRow
import com.uxankit.stepwise.ui.components.animateRow
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import com.uxankit.stepwise.util.DateText

/** IA 3 · Inbox: all saved tasks. Tap one to plan it (Move to Today, Mark as urgent, Delete). */
@Composable
fun InboxTab(
    tasks: List<Task>,
    onProfile: () -> Unit,
    onPlan: (Task) -> Unit,
    onToggleDone: (Task, Boolean) -> Unit,
) {
    val colors = StepwiseTheme.colors
    val inbox = Planner.inbox(tasks)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            TabHeader(
                overline = if (inbox.size == 1) "1 saved task" else "${inbox.size} saved tasks",
                title = "Inbox",
                onProfile = onProfile,
            )
        }
        item(key = "hint") {
            Text(
                "Everything you’ve saved. Tap a task to plan it.",
                style = StepwiseTheme.type.small,
                color = colors.muted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        if (inbox.isEmpty()) {
            item(key = "empty") {
                StepCard(animateRow()) {
                    Text("Your Inbox is empty", style = StepwiseTheme.type.bodyLargeMedium, color = colors.ink)
                    Text("Tap + to capture anything on your mind. Only the name is needed.", style = StepwiseTheme.type.body, color = colors.muted)
                }
            }
        }
        items(inbox, key = { it.id }) { task ->
            TaskRow(
                title = task.title,
                subtitle = if (task.hasSteps) "${task.steps.size} steps" else DateText.added(task.createdAt),
                checked = false,
                onToggle = { onToggleDone(task, true) },
                onClick = { onPlan(task) },
                modifier = animateRow(),
                sound = UiSound.Open, // opens the plan sheet
            )
        }
    }
}
