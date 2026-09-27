package com.uxankit.stepwise.ui.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.uxankit.stepwise.data.model.UserSettings
import com.uxankit.stepwise.resources.Res
import com.uxankit.stepwise.resources.inter_medium
import com.uxankit.stepwise.resources.inter_regular
import org.jetbrains.compose.resources.Font

/** MindMarket tokens (DESIGN.md). Cream canvas, white cards, one green brand accent. */
@Immutable
data class StepwiseColors(
    val canvas: Color = Color(0xFFF5F1E4),
    val surface: Color = Color(0xFFFFFFFF),
    val recessed: Color = Color(0xFFE0DBCE),
    val ink: Color = Color(0xFF2C2E2A),
    val muted: Color = Color(0xFF80827F),
    val hairline: Color = Color(0xFFD5D5D4),
    val grass: Color = Color(0xFF8ED462),
    val sky: Color = Color(0xFF2BA0FF),
    val coral: Color = Color(0xFFFF705D),
    val sunshine: Color = Color(0xFFF5E211),
    val onInk: Color = Color(0xFFFFFFFF),
    val scrim: Color = Color(0x662C2E2A),
)

private val HighContrastColors = StepwiseColors(
    muted = Color(0xFF55574F),
    hairline = Color(0xFF9C9D99),
    recessed = Color(0xFFCFC8B6),
)

@Immutable
data class StepwiseType(
    /** 53px display used for the one big thing on a screen (Welcome, Focus Mode). */
    val hero: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val bodyLarge: TextStyle,
    val bodyLargeMedium: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val small: TextStyle,
)

@Composable
private fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
)

@Composable
private fun stepwiseType(): StepwiseType {
    val inter = interFamily()
    fun style(size: Int, weight: FontWeight, lineHeight: Float, tracking: Float = 0f) = TextStyle(
        fontFamily = inter,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        letterSpacing = tracking.em,
    )
    return StepwiseType(
        hero = style(53, FontWeight.Medium, 1.05f, -0.04f),
        title = style(30, FontWeight.Medium, 1.2f, -0.01f),
        section = style(20, FontWeight.Medium, 1.25f),
        bodyLarge = style(17, FontWeight.Normal, 1.5f),
        bodyLargeMedium = style(17, FontWeight.Medium, 1.3f),
        body = style(15, FontWeight.Normal, 1.4f),
        label = style(15, FontWeight.Medium, 1.3f),
        caption = style(13, FontWeight.Medium, 1.25f),
        small = style(13, FontWeight.Normal, 1.35f),
    )
}

val LocalStepwiseColors = staticCompositionLocalOf { StepwiseColors() }
val LocalStepwiseType = staticCompositionLocalOf<StepwiseType> { error("StepwiseTheme missing") }
val LocalUserSettings = staticCompositionLocalOf { UserSettings() }

object StepwiseTheme {
    val colors: StepwiseColors @Composable get() = LocalStepwiseColors.current
    val type: StepwiseType @Composable get() = LocalStepwiseType.current
    val settings: UserSettings @Composable get() = LocalUserSettings.current
}

@Composable
fun StepwiseTheme(settings: UserSettings = UserSettings(), content: @Composable () -> Unit) {
    val colors = remember(settings.higherContrast) {
        if (settings.higherContrast) HighContrastColors else StepwiseColors()
    }
    val type = stepwiseType()
    val material = lightColorScheme(
        primary = colors.ink,
        onPrimary = colors.onInk,
        secondary = colors.grass,
        background = colors.canvas,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.canvas,
        onSurfaceVariant = colors.muted,
        outline = colors.hairline,
        error = colors.coral,
    )
    CompositionLocalProvider(
        LocalStepwiseColors provides colors,
        LocalStepwiseType provides type,
        LocalUserSettings provides settings,
        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.ink,
            backgroundColor = colors.grass.copy(alpha = 0.4f),
        ),
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}
