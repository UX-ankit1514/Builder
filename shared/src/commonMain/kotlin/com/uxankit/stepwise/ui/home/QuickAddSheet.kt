package com.uxankit.stepwise.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepwiseSheet
import com.uxankit.stepwise.ui.components.StepwiseTextField
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/** 2.2 Quick add. */
@Composable
fun QuickAddSheet(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    fun save() {
        if (title.isNotBlank()) {
            onSave(title)
            onDismiss()
        }
    }
    StepwiseSheet(onDismiss = onDismiss, title = "Quick add") {
        StepwiseTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "What’s on your mind?",
            maxLength = Limits.TITLE_MAX,
            onImeAction = ::save,
            accessibilityLabel = "Task name",
            modifier = Modifier.focusRequester(focus),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(StepIcons.Inbox, contentDescription = null, tint = StepwiseTheme.colors.muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Just the name. It goes to your Inbox.", style = StepwiseTheme.type.small, color = StepwiseTheme.colors.muted)
        }
        ServiceCta(text = "Save", onClick = ::save, enabled = title.isNotBlank())
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
