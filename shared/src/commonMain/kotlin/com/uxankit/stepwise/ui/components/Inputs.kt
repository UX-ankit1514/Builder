package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** Pill text field: hairline border at rest, ink border while typing. */
@Composable
fun StepwiseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLength: Int = 200,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    accessibilityLabel: String = placeholder,
) {
    val colors = StepwiseTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = if (singleLine) CircleShape else RoundedCornerShape(24.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        textStyle = StepwiseTheme.type.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.ink),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(colors.surface)
                    .border(if (focused) 1.5.dp else 1.dp, if (focused) colors.ink else colors.hairline, shape)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = StepwiseTheme.type.bodyLarge, color = colors.muted)
                inner()
            }
        },
    )
}
