package com.uxankit.stepwise.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * Motion tokens: the transitions.dev scale (durations, easings, distances, blur) plus the
 * better-ui press and icon values. Pick a token by what the motion does, not by the nearest number.
 * Everything that moves reads StepwiseTheme.settings.reduceMotion and snaps when it is on.
 */
object Motion {
    /** Per-item stagger offset. */
    const val STAGGER = 40
    /** Intent delay (check path draw), stagger between a few large blocks. */
    const val MICRO = 80
    /** Closes, text swaps, press feedback, colour changes. */
    const val QUICK = 150
    /** Opens, tabs sliding, page slide, accordion. */
    const val FAST = 250
    /** Toast open. */
    const val MEDIUM = 350
    /** Emphasis: success check, text reveal, number pop-in, badge appear. */
    const val VERY_SLOW = 500

    /** better-ui contextual icon swap. */
    const val ICON = 300
    /** Checkbox: box fill, then the check draws. Unchecking reverses on QUICK. */
    const val CHECK_BOX = 150
    const val CHECK_DRAW = 350

    /** Surfaces: open, close, page slide, resize, position change. The default. */
    val SmoothOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    /** CSS ease-in-out: text swap. */
    val InOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    /** CSS ease-out: press feedback. */
    val Out: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    /** Overshoot for entrances only (badge pop, number pop-in, check bob). Never on a close. */
    val Bounce: Easing = CubicBezierEasing(0.34f, 1.36f, 0.64f, 1f)
    /** better-ui icon cross-fade. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val DistanceMicro = 4.dp // text swap
    val DistanceBase = 8.dp // page slide, number pop-in
    val DistanceMedium = 12.dp // text reveal
    val DistanceToast = 16.dp
    val DistanceCheckBob = 40.dp // success check Y-bob

    val BlurSmall = 2.dp // text swap, number pop-in
    val BlurMedium = 3.dp // text reveal
    val BlurIcon = 4.dp // better-ui icon swap
    val BlurLarge = 8.dp // success check open

    /** better-ui scale on press. Never below 0.95. */
    const val PRESS_SCALE = 0.96f
    /** better-ui icon swap: icons grow from a quarter size. */
    const val ICON_SCALE = 0.25f
    const val TOAST_SCALE = 0.97f
    /** Success check rotates upright from here. */
    const val CHECK_ROTATE = 80f

    /**
     * Tabs sliding, for a target that moves while it animates (the tab widths change too).
     * Settles in about FAST like SmoothOut, but keeps its speed when retargeted instead of restarting.
     */
    fun <T> slideSpring(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
}
