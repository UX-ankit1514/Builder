package com.uxankit.stepwise.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.ui.components.CardShape
import com.uxankit.stepwise.ui.components.PopNumber
import com.uxankit.stepwise.ui.components.SectionHeader
import com.uxankit.stepwise.ui.components.SegmentedProgress
import com.uxankit.stepwise.ui.components.StepCard
import com.uxankit.stepwise.ui.components.TabHeader
import com.uxankit.stepwise.ui.components.animateRow
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import com.uxankit.stepwise.util.DateText
import com.uxankit.stepwise.util.Time

/** IA 4 · Progress: steps completed, active goals, history. Progress over punishment. */
@Composable
fun ProgressTab(tasks: List<Task>, onProfile: () -> Unit, onOpenTask: (String) -> Unit) {
    val colors = StepwiseTheme.colors
    val today = Time.today()
    val goals = Planner.activeGoals(tasks)
    val stepsThisWeek = Planner.stepsDoneInWeek(tasks, today)
    val week = Time.weekOf(today)

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
        item(key = "header") { TabHeader(overline = "This week", title = "Progress", onProfile = onProfile) }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stepsThisWeek, if (stepsThisWeek == 1) "Step completed" else "Steps completed", colors.surface, Modifier.weight(1f))
                StatCard(goals.size, if (goals.size == 1) "Active goal" else "Active goals", colors.grass, Modifier.weight(1f))
            }
        }
        item(key = "goals-header") { SectionHeader("Active goals", animateRow().padding(top = 8.dp)) }
        if (goals.isEmpty()) {
            item(key = "goals-empty") {
                Text(
                    "Break a big task into steps and it shows up here.",
                    style = StepwiseTheme.type.body,
                    color = colors.muted,
                    modifier = animateRow().padding(horizontal = 4.dp),
                )
            }
        }
        items(goals, key = { it.id }) { task ->
            StepCard(animateRow(), onClick = { onOpenTask(task.id) }, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        task.title,
                        style = StepwiseTheme.type.bodyLargeMedium,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    Text("${task.stepsDone} of ${task.steps.size} steps", style = StepwiseTheme.type.body, color = colors.muted)
                }
                SegmentedProgress(total = task.steps.size, done = task.stepsDone, markCurrent = false)
            }
        }
        item(key = "history-header") { SectionHeader("History", animateRow().padding(top = 8.dp)) }
        item(key = "history") {
            StepCard(animateRow(), spacing = 14) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        val count = Planner.stepsDoneOn(tasks, day)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = "${DateText.weekdayShort(day.dayOfWeek)}: $count steps"
                            },
                        ) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(if (count > 0) colors.grass else colors.canvas),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (count > 0) Text("$count", style = StepwiseTheme.type.caption, color = colors.ink)
                            }
                            Text(
                                DateText.weekdayLetter(day.dayOfWeek),
                                style = StepwiseTheme.type.small,
                                color = if (day == today) colors.ink else colors.muted,
                                modifier = Modifier.padding(top = 6.dp).clearAndSetSemantics { },
                            )
                        }
                    }
                }
                Text("Numbers show steps done each day. Rest days are fine.", style = StepwiseTheme.type.body, color = colors.muted)
            }
        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    val colors = StepwiseTheme.colors
    Column(
        modifier = modifier
            .height(110.dp)
            .clip(CardShape)
            .background(color)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        PopNumber(
            value,
            style = StepwiseTheme.type.hero.copy(fontSize = StepwiseTheme.type.hero.fontSize * 0.9f),
            color = colors.ink,
        )
        Text(label, style = StepwiseTheme.type.body, color = colors.ink)
    }
}
