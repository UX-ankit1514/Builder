package com.uxankit.stepwise.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/**
 * The 53px display headline. The design is drawn on a 390dp-wide column; on
 * narrower phones the size scales down a little so long steps still fit.
 */
@Composable
fun HeroText(text: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scale = (maxWidth / 390.dp).coerceIn(0.78f, 1f)
        val base = StepwiseTheme.type.hero
        Text(
            text = text,
            style = base.copy(fontSize = base.fontSize * scale, lineHeight = base.lineHeight * scale),
            color = StepwiseTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}
