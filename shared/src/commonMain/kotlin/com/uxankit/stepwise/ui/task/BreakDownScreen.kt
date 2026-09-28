package com.uxankit.stepwise.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.components.HeaderAction
import com.uxankit.stepwise.ui.components.NavHeader
import com.uxankit.stepwise.ui.components.ScreenScaffold
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** 4.3 "Break Down — add steps myself". Also used to edit existing steps. */
@Composable
fun BreakDownScreen(taskId: String, onClose: () -> Unit) {
    val user = requireUser()
    val vm = viewModel { TaskViewModel(user.uid, taskId) }
    val state by vm.state.collectAsState()
    val task = (state as? TaskState.Ready)?.task
    var steps by remember { mutableStateOf<List<EditableStep>?>(null) }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(task?.id) {
        if (steps == null && task != null) steps = task.toEditableSteps()
    }
    LaunchedEffect(state) {
        if (state == TaskState.Missing) onClose()
    }

    val current = steps.orEmpty()
    val toSave = current.withDraft(draft)
    val colors = StepwiseTheme.colors
    ScreenScaffold(
        bottom = {
            ServiceCta(
                text = when (toSave.size) {
                    0 -> "Save steps"
                    1 -> "Save 1 step"
                    else -> "Save ${toSave.size} steps"
                },
                enabled = toSave.isNotEmpty() || (task?.hasSteps == true),
                sound = UiSound.Save,
                onClick = {
                    vm.saveSteps(toSave.toStepPairs())
                    onClose()
                },
            )
        },
    ) {
        NavHeader(title = "Break it down", leading = HeaderAction(StepIcons.Close, "Close", UiSound.Close, onClose))
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task?.title.orEmpty(), style = StepwiseTheme.type.title, color = colors.ink)
            Text(
                "Make each step one small action you could start now. Begin with a verb.",
                style = StepwiseTheme.type.small,
                color = colors.muted,
            )
        }
        // Wait for the saved steps, so the editor treats them as already there rather than newly added.
        if (steps != null) {
            StepsEditor(steps = current, onChange = { steps = it }, draft = draft, onDraftChange = { draft = it })
        }
    }
}
