package com.uxankit.stepwise.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.sound.rememberUiSounds
import com.uxankit.stepwise.ui.theme.LocalUserSettings
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/*
 * Shared motion: press feedback, drawn checks, icon / text / number swaps, staggered reveals
 * and page slides. Values come from ui/theme/Motion. Colour and opacity fades stay on with
 * Reduce motion; anything that moves, scales or blurs snaps instead.
 */

// ---- Scale on press (better-ui) ----------------------------------------------------------

/**
 * Scales the element to 0.96 while [interactionSource] is pressed. A quick tap still plays the
 * whole press before letting go; a press that turns into a scroll lets go at once.
 */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = this then PressScaleElement(interactionSource)

/**
 * clickable plus the 0.96 press scale and a UI [sound] on click. Put it first in the chain so the
 * whole control scales, before background and border. [shape] clips the ripple. [static] turns
 * the scale off where motion would distract.
 */
@Composable
fun Modifier.pressClickable(
    shape: Shape,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    static: Boolean = false,
    sound: UiSound? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val sounds = rememberUiSounds()
    return this
        .then(if (static) Modifier else Modifier.pressScale(interaction))
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        ) {
            sounds.play(sound)
            onClick()
        }
}

private data class PressScaleElement(val interactionSource: InteractionSource) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource)
    override fun update(node: PressScaleNode) = node.update(interactionSource)
}

private class PressScaleNode(private var interactionSource: InteractionSource) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val scale = Animatable(1f)
    private var collector: Job? = null
    private val layer: GraphicsLayerScope.() -> Unit = {
        scaleX = scale.value
        scaleY = scale.value
    }

    override fun onAttach() {
        collect()
    }

    fun update(source: InteractionSource) {
        if (source == interactionSource) return
        interactionSource = source
        collector?.cancel()
        if (isAttached) collect()
    }

    private fun collect() {
        collector = coroutineScope.launch {
            val spec = tween<Float>(Motion.QUICK, easing = Motion.Out)
            var pressIn: Job? = null
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> if (!currentValueOf(LocalUserSettings).reduceMotion) {
                        pressIn = launch { scale.animateTo(Motion.PRESS_SCALE, spec) }
                    }
                    is PressInteraction.Release -> {
                        val pressing = pressIn
                        launch {
                            pressing?.join()
                            scale.animateTo(1f, spec)
                        }
                    }
                    is PressInteraction.Cancel -> {
                        pressIn?.cancel()
                        launch { scale.animateTo(1f, spec) }
                    }
                }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layer)
        }
    }
}

// ---- Checks (transitions.dev checkbox check / success check) ----------------------------

/**
 * The Check icon's stroke, drawn from the short arm to the tip up to [progress] (0..1).
 * [progress] is read at draw time only, so animating it never recomposes.
 */
@Composable
fun DrawnCheck(progress: () -> Float, color: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier.drawWithCache {
            val unit = size.minDimension / 24f
            val left = (size.width - 24f * unit) / 2
            val top = (size.height - 24f * unit) / 2
            val full = Path().apply {
                moveTo(left + 4f * unit, top + 12f * unit)
                lineTo(left + 9f * unit, top + 17f * unit)
                lineTo(left + 20f * unit, top + 6f * unit)
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            val segment = Path()
            val stroke = Stroke(width = 1.75f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                val p = progress().coerceIn(0f, 1f)
                if (p > 0f) {
                    segment.reset()
                    measure.getSegment(0f, measure.length * p, segment, true)
                    drawPath(segment, color, style = stroke)
                }
            }
        },
    )
}

/** A check that draws itself in when [checked] turns on and pulls back quickly when it turns off. */
@Composable
fun CheckMark(checked: Boolean, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val progress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = when {
            reduceMotion -> snap()
            checked -> tween(Motion.CHECK_DRAW, easing = Motion.SmoothOut)
            else -> tween(Motion.QUICK, easing = Motion.SmoothOut)
        },
        label = "check draw",
    )
    DrawnCheck({ progress.value }, color, modifier)
}

// ---- Swaps ---------------------------------------------------------------------------------

/** Blurs by [radius] px, read at draw time. Android 11 and older draw it sharp. */
fun Modifier.blurLayer(radius: () -> Float): Modifier = graphicsLayer {
    val r = radius()
    renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
}

/**
 * Cross-fades what sits in one slot (spinner ↔ arrow dot): the new one grows from a quarter
 * size, fades and un-blurs while the old one does the reverse (better-ui contextual icons).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> IconSwap(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val blur = with(LocalDensity.current) { Motion.BlurIcon.toPx() }
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            if (reduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val spec = tween<Float>(Motion.ICON, easing = Motion.Emphasized)
                (fadeIn(spec) + scaleIn(spec, initialScale = Motion.ICON_SCALE)) togetherWith
                    (fadeOut(spec) + scaleOut(spec, targetScale = Motion.ICON_SCALE)) using SizeTransform(clip = false)
            }
        },
        label = "icon swap",
    ) { value ->
        val radius by transition.animateFloat(
            transitionSpec = { tween(Motion.ICON, easing = Motion.Emphasized) },
            label = "icon blur",
        ) { state -> if (state == EnterExitState.Visible || reduceMotion) 0f else blur }
        Box(Modifier.blurLayer { radius }) { content(value) }
    }
}

/** Text that changes in place: the old words lift out, then the new ones rise in (text states swap). */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SwapText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val density = LocalDensity.current
    val lift = with(density) { Motion.DistanceMicro.roundToPx() }
    val blur = with(density) { Motion.BlurSmall.toPx() }
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val fadeOutSpec = tween<Float>(Motion.QUICK, easing = Motion.InOut)
                val moveOutSpec = tween<IntOffset>(Motion.QUICK, easing = Motion.InOut)
                val fadeInSpec = tween<Float>(Motion.QUICK, delayMillis = Motion.QUICK, easing = Motion.InOut)
                val moveInSpec = tween<IntOffset>(Motion.QUICK, delayMillis = Motion.QUICK, easing = Motion.InOut)
                (fadeIn(fadeInSpec) + slideInVertically(moveInSpec) { lift }) togetherWith
                    (fadeOut(fadeOutSpec) + slideOutVertically(moveOutSpec) { -lift }) using SizeTransform(clip = false)
            }
        },
        label = "text swap",
    ) { value ->
        val radius by transition.animateFloat(
            transitionSpec = { tween(Motion.QUICK, easing = Motion.InOut) },
            label = "text blur",
        ) { state -> if (state == EnterExitState.Visible || reduceMotion) 0f else blur }
        Text(value, style = style, color = color, textAlign = textAlign, modifier = Modifier.blurLayer { radius })
    }
}

/** A number that re-enters with a blurred rise when it changes (number pop-in). Counting down drops in from above. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PopNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val density = LocalDensity.current
    val rise = with(density) { Motion.DistanceBase.roundToPx() }
    val blur = with(density) { Motion.BlurSmall.toPx() }
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val from = if (targetState >= initialState) rise else -rise
                (fadeIn(tween(Motion.VERY_SLOW, easing = Motion.SmoothOut)) +
                    slideInVertically(tween(Motion.VERY_SLOW, easing = Motion.Bounce)) { from }) togetherWith
                    fadeOut(tween(Motion.QUICK, easing = Motion.SmoothOut)) using SizeTransform(clip = false)
            }
        },
        label = "number pop-in",
    ) { number ->
        val radius by transition.animateFloat(
            transitionSpec = { tween(Motion.VERY_SLOW, easing = Motion.SmoothOut) },
            label = "number blur",
        ) { state -> if (state == EnterExitState.Visible || reduceMotion) 0f else blur }
        Text("$number", style = style, color = color, modifier = Modifier.blurLayer { radius })
    }
}

// ---- Entrances and page slides -------------------------------------------------------------

/**
 * One block of a staggered entrance (texts reveal): rises 12dp, un-blurs and fades in once,
 * when it first appears. [index] holds it back [stagger] ms per block before it.
 */
@Composable
fun Modifier.reveal(index: Int = 0, stagger: Int = Motion.STAGGER, delay: Int = 0): Modifier {
    if (StepwiseTheme.settings.reduceMotion) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(Motion.VERY_SLOW, delayMillis = delay + index * stagger, easing = Motion.SmoothOut))
    }
    val density = LocalDensity.current
    val rise = with(density) { Motion.DistanceMedium.toPx() }
    val blur = with(density) { Motion.BlurMedium.toPx() }
    return graphicsLayer {
        val remaining = 1f - progress.value
        alpha = progress.value
        translationY = rise * remaining
        val r = blur * remaining
        renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
    }
}

/** List rows fade in, fade out and glide to their new place when the list changes. Items need stable keys. */
@Composable
fun LazyItemScope.animateRow(): Modifier =
    if (StepwiseTheme.settings.reduceMotion) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = tween(Motion.FAST, easing = Motion.SmoothOut),
            placementSpec = tween(Motion.FAST, easing = Motion.SmoothOut),
            fadeOutSpec = tween(Motion.QUICK, easing = Motion.SmoothOut),
        )
    }

/** Page side-by-side: the new page slides [distancePx] in from the side it came from. 250ms both ways. */
fun pageEnter(forward: Boolean, distancePx: Int): EnterTransition =
    fadeIn(tween(Motion.FAST, easing = Motion.SmoothOut)) +
        slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) distancePx else -distancePx }

/** The page being left slides the other way while it fades. */
fun pageExit(forward: Boolean, distancePx: Int): ExitTransition =
    fadeOut(tween(Motion.FAST, easing = Motion.SmoothOut)) +
        slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) -distancePx else distancePx }

fun pageSlide(forward: Boolean, distancePx: Int): ContentTransform =
    pageEnter(forward, distancePx) togetherWith pageExit(forward, distancePx)
