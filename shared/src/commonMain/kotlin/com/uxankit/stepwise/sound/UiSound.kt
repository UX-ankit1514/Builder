package com.uxankit.stepwise.sound

/**
 * The UI sound kit (ui-sounds.js): every sound is synthesized from a small recipe of layers,
 * with no audio files. [SoundSynth] renders a recipe the way the Web Audio kit plays it.
 *
 * Layer fields match the kit: a fixed pitch or a glide from → to over the sound's length,
 * an [Env] of attack / decay / release in seconds (sustain is always 0, a "pluck"), a start
 * delay, optional FM wobble and an optional filter.
 */
enum class UiSound(vararg val layers: Layer) {
    /** Solid buttons: the coral Service CTA. */
    Press(
        noise(Noise.White, bandPass(2100.0, 0.9), env(0.0, 0.006, 0.003), gain = 0.124),
        tone(Wave.Triangle, 520.0, 430.0, env(0.001, 0.038, 0.014), gain = 0.698),
        tone(Wave.Sine, 1220.0, 880.0, env(0.001, 0.018, 0.007), gain = 0.203, fm = Fm(0.5, 35.0)),
    ),

    /** Outline buttons: the Primary CTA pill and the white circle buttons. */
    PressOutline(
        noise(Noise.White, bandPass(2950.0, 1.2), env(0.0, 0.005, 0.002), gain = 0.203),
        tone(Wave.Sine, 1250.0, 970.0, env(0.001, 0.025, 0.008), gain = 0.625, fm = Fm(0.5, 48.0)),
        tone(Wave.Triangle, 220.0, 150.0, env(0.0, 0.018, 0.006), gain = 0.187),
    ),

    /** Soft / raised buttons: chips, Focus Mode's round actions, soft sheet buttons. */
    PressSoft(
        noise(Noise.Pink, bandPass(1980.0, 0.85), env(0.0, 0.007, 0.003), gain = 0.111),
        tone(Wave.Sine, 760.0, 580.0, env(0.001, 0.034, 0.012), gain = 0.576, fm = Fm(0.5, 24.0), filter = lowPass(2600.0, 0.7)),
        tone(Wave.Sine, 1420.0, 1100.0, env(0.001, 0.016, 0.006), gain = 0.155),
    ),

    /** Ghost buttons. */
    PressGhost(*ghost()),

    /** Ghost press in "navigation" mode: tabs, back, links, rows that open a page. Cuts off anything still ringing. */
    Navigate(*ghost()),

    /** Two quick rising blips: something was saved. */
    Save(
        tone(Wave.Sine, 800.0, 1200.0, env(0.0, 0.03, 0.01), gain = 0.5),
        tone(Wave.Sine, 1000.0, 1500.0, env(0.0, 0.03, 0.01), gain = 0.42, delay = 0.05),
    ),

    /** The same idea falling: something was removed or taken back. */
    Unsave(
        tone(Wave.Sine, 1500.0, 950.0, env(0.0, 0.04, 0.01), gain = 0.392),
        tone(Wave.Sine, 1150.0, 750.0, env(0.0, 0.04, 0.01), gain = 0.329, delay = 0.05),
    ),

    /** Two even blips: a copy was made (export). */
    Copy(
        tone(Wave.Sine, 1200.0, env = env(0.0, 0.015, 0.006), gain = 0.627),
        tone(Wave.Sine, 1400.0, env = env(0.0, 0.015, 0.006), gain = 0.549, delay = 0.04),
    ),

    /** Checkbox / radio. */
    Pop(tone(Wave.Sine, 300.0, 2000.0, env(0.008, 0.12, 0.04), gain = 0.326)),

    /** Menu item or option picked. */
    Select(tone(Wave.Sine, 860.0, 1040.0, env(0.001, 0.05, 0.02), gain = 0.5)),

    ToggleOn(
        tone(Wave.Sine, 520.0, env = env(0.001, 0.065, 0.02), gain = 0.315),
        tone(Wave.Sine, 760.0, env = env(0.001, 0.07, 0.02), gain = 0.315, delay = 0.035),
    ),

    ToggleOff(
        tone(Wave.Sine, 620.0, env = env(0.001, 0.055, 0.018), gain = 0.352),
        tone(Wave.Sine, 410.0, env = env(0.001, 0.055, 0.018), gain = 0.352, delay = 0.03),
    ),

    /** Menu, sheet or dialog opens. */
    Open(tone(Wave.Sine, 420.0, 760.0, env(0.002, 0.09, 0.03), gain = 0.392)),

    /** Menu, sheet or dialog closes. */
    Close(tone(Wave.Sine, 720.0, 360.0, env(0.002, 0.08, 0.025), gain = 0.408)),

    /** A rising three-note chord: a step or task is done. */
    Success(
        tone(Wave.Sine, 523.0, env = env(0.003, 0.3, 0.1), gain = 0.124),
        tone(Wave.Sine, 659.0, env = env(0.003, 0.28, 0.1), gain = 0.109, delay = 0.07),
        tone(Wave.Sine, 784.0, 880.0, env(0.003, 0.32, 0.12), gain = 0.116, delay = 0.14),
    ),

    Error(
        tone(Wave.Sawtooth, 320.0, 140.0, env(0.0, 0.25, 0.08), gain = 0.261, filter = lowPass(1200.0, 1.0)),
        tone(Wave.Square, 180.0, 80.0, env(0.0, 0.2, 0.06), gain = 0.142, delay = 0.03, filter = lowPass(800.0, 1.0)),
    ),
    ;

    /** "navigation" mode: fade out whatever is still ringing so quick taps never pile up. */
    val cutsOff: Boolean get() = this == Navigate
}

enum class Wave { Sine, Triangle, Square, Sawtooth }

enum class Noise { White, Pink }

enum class FilterType { LowPass, BandPass }

class Env(val attack: Double, val decay: Double, val release: Double)

/** A sine at ratio × pitch that wobbles the pitch by ±depth Hz: the metallic "tick". */
class Fm(val ratio: Double, val depth: Double)

class Filter(val type: FilterType, val frequency: Double, val q: Double)

/** One oscillator or noise voice of a sound. [noise] set means a noise burst and [wave] is ignored. */
class Layer(
    val wave: Wave,
    val noise: Noise?,
    val from: Double,
    val to: Double,
    val env: Env,
    val gain: Double,
    val delay: Double,
    val fm: Fm?,
    val filter: Filter?,
) {
    /** Envelope length; the voice itself stops 0.1s later, as in the kit. */
    val length: Double get() = env.attack + env.decay + env.release
}

private fun tone(
    wave: Wave,
    from: Double,
    to: Double = from,
    env: Env,
    gain: Double,
    delay: Double = 0.0,
    fm: Fm? = null,
    filter: Filter? = null,
) = Layer(wave, noise = null, from, to, env, gain, delay, fm, filter)

private fun noise(color: Noise, filter: Filter, env: Env, gain: Double) =
    Layer(Wave.Sine, color, from = 0.0, to = 0.0, env, gain, delay = 0.0, fm = null, filter)

private fun env(attack: Double, decay: Double, release: Double) = Env(attack, decay, release)

private fun bandPass(frequency: Double, q: Double) = Filter(FilterType.BandPass, frequency, q)

private fun lowPass(frequency: Double, q: Double) = Filter(FilterType.LowPass, frequency, q)

private fun ghost() = arrayOf(
    noise(Noise.White, bandPass(7200.0, 1.1), env(0.0, 0.004, 0.002), gain = 0.25),
    tone(Wave.Sine, 2200.0, 1800.0, env(0.0, 0.014, 0.005), gain = 0.683, fm = Fm(0.5, 42.0)),
    tone(Wave.Triangle, 320.0, 240.0, env(0.0, 0.021, 0.008), gain = 0.25),
)
