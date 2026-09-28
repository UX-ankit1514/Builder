package com.uxankit.stepwise.ui.task

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.sound.rememberUiSounds
import com.uxankit.stepwise.ui.components.CircleIconButton
import com.uxankit.stepwise.ui.components.StepwiseTextField
import com.uxankit.stepwise.ui.components.reveal
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** A step while it is being edited. [id] is null for new steps. */
data class EditableStep(val key: Int, val id: String?, val title: String)

fun Task.toEditableSteps(): List<EditableStep> = steps.mapIndexed { i, step -> EditableStep(i, step.id, step.title) }

fun List<EditableStep>.toStepPairs(): List<Pair<String?, String>> = map { it.id to it.title }

/** Numbered, editable step list plus an "Add step n" field. Return adds the next step. */
@Composable
fun StepsEditor(
    steps: List<EditableStep>,
    onChange: (List<EditableStep>) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numberPrefix: String = "",
    showHint: Boolean = true,
) {
    val colors = StepwiseTheme.colors
    val sounds = rememberUiSounds()
    fun add() {
        if (draft.isBlank() || steps.size >= Limits.STEPS_MAX) return
        sounds.play(UiSound.Select)
        onChange(steps.withDraft(draft))
        onDraftChange("")
    }

    // Steps that were already there when the editor opened don't animate; new ones rise in.
    val initialKeys = remember { steps.map { it.key }.toSet() }
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    Column(
        modifier
            .fillMaxWidth()
            .then(if (reduceMotion) Modifier else Modifier.animateContentSize(tween(Motion.FAST, easing = Motion.SmoothOut))),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        steps.forEachIndexed { index, step ->
            key(step.key) {
                StepEditRow(
                    number = "$numberPrefix${index + 1}",
                    step = step,
                    canMoveUp = index > 0,
                    canMoveDown = index < steps.lastIndex,
                    onTitle = { title -> onChange(steps.map { if (it.key == step.key) it.copy(title = title) else it }) },
                    onMove = { delta ->
                        val list = steps.toMutableList()
                        val item = list.removeAt(index)
                        list.add((index + delta).coerceIn(0, list.size), item)
                        onChange(list)
                    },
                    onRemove = { onChange(steps.filterNot { it.key == step.key }) },
                    modifier = if (step.key in initialKeys) Modifier else Modifier.reveal(),
                )
            }
        }
        StepwiseTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = "Add step $numberPrefix${steps.size + 1}…",
            onImeAction = ::add,
            accessibilityLabel = "Add a step",
        )
        if (showHint) {
            Text(
                "+ Press return to add step $numberPrefix${steps.size + 1} · 3–7 steps work best",
                style = StepwiseTheme.type.small,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/** The steps plus the half-typed "Add step" text, so Save never drops the last one. */
fun List<EditableStep>.withDraft(draft: String): List<EditableStep> =
    if (draft.isBlank()) this else this + EditableStep((maxOfOrNull { it.key } ?: -1) + 1, null, draft.trim())

@Composable
private fun StepEditRow(
    number: String,
    step: EditableStep,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTitle: (String) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StepwiseTheme.colors
    val sounds = rememberUiSounds()
    var menu by remember { mutableStateOf(false) }
    fun pick(sound: UiSound, action: () -> Unit) {
        sounds.play(sound)
        menu = false
        action()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(StepIcons.Grip, contentDescription = null, tint = colors.hairline, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(colors.recessed),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = StepwiseTheme.type.small.copy(fontSize = StepwiseTheme.type.small.fontSize * 0.85f), color = colors.ink)
        }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = step.title,
            onValueChange = { onTitle(it.take(Limits.TITLE_MAX)) },
            textStyle = StepwiseTheme.type.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp).semantics { contentDescription = "Step $number" },
        )
        Box {
            CircleIconButton(
                StepIcons.More,
                contentDescription = "Step $number options",
                onClick = { menu = true },
                size = 40.dp,
                iconSize = 18.dp,
                sound = UiSound.Open,
            )
            DropdownMenu(expanded = menu, onDismissRequest = { pick(UiSound.Close) {} }, containerColor = colors.surface) {
                if (canMoveUp) DropdownMenuItem(text = { Text("Move up") }, onClick = { pick(UiSound.Select) { onMove(-1) } })
                if (canMoveDown) DropdownMenuItem(text = { Text("Move down") }, onClick = { pick(UiSound.Select) { onMove(1) } })
                DropdownMenuItem(text = { Text("Remove step") }, onClick = { pick(UiSound.Unsave, onRemove) })
            }
        }
    }
}
