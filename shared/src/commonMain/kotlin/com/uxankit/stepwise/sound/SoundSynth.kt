package com.uxankit.stepwise.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders a [UiSound] recipe to mono float PCM, following the Web Audio graph in ui-sounds.js:
 * oscillator or noise → optional biquad filter → gain envelope, layers summed and clipped
 * like the audio output. Each platform renders every sound once and plays it from memory.
 */
object SoundSynth {
    const val SAMPLE_RATE = 48_000

    /** Exponential ramps can't reach 0. */
    private const val SILENT = 0.0001

    /** Sources stop 0.1s after the envelope's length. */
    private const val TAIL = 0.1

    fun render(sound: UiSound, sampleRate: Int = SAMPLE_RATE): FloatArray {
        val end = sound.layers.maxOf { it.delay + it.length + TAIL }
        val mix = DoubleArray((end * sampleRate).roundToInt() + 1)
        // Seeded, so a sound's noise is the same every launch (the kit caches one buffer too).
        val random = Random(sound.ordinal + 1)
        sound.layers.forEach { renderLayer(it, mix, sampleRate, random) }
        return FloatArray(mix.size) { mix[it].coerceIn(-1.0, 1.0).toFloat() }
    }

    private fun renderLayer(layer: Layer, mix: DoubleArray, sampleRate: Int, random: Random) {
        val start = (layer.delay * sampleRate).roundToInt()
        val frames = ((layer.length + TAIL) * sampleRate).roundToInt()
        val noise = layer.noise?.let { noiseBuffer(it, frames, random) }
        val filter = layer.filter?.let { Biquad(it, sampleRate) }
        var phase = 0.0
        var modPhase = 0.0
        for (i in 0 until frames) {
            val index = start + i
            if (index >= mix.size) break
            val t = i.toDouble() / sampleRate
            var sample = if (noise != null) {
                noise[i]
            } else {
                var frequency = layer.frequencyAt(t)
                layer.fm?.let { fm ->
                    frequency += fm.depth * sin(2 * PI * modPhase)
                    modPhase += layer.from * fm.ratio / sampleRate
                }
                val step = frequency / sampleRate
                val value = oscillator(layer.wave, phase, step)
                phase += step
                phase -= floor(phase)
                value
            }
            if (filter != null) sample = filter.process(sample)
            mix[index] += sample * layer.gainAt(t)
        }
    }

    /** exponentialRampToValueAtTime from → to over the envelope length, then hold. */
    private fun Layer.frequencyAt(t: Double): Double = when {
        to == from -> from
        t >= length -> to
        else -> from * (maxOf(to, 1.0) / from).pow(t / length)
    }

    /** Linear attack from silence to the peak, then setTargetAtTime toward silence with τ = decay / 3. */
    private fun Layer.gainAt(t: Double): Double {
        val attack = env.attack
        return if (attack > 0 && t < attack) {
            SILENT + (gain - SILENT) * t / attack
        } else {
            SILENT + (gain - SILENT) * exp(-(t - attack) / (env.decay / 3))
        }
    }

    /** Web Audio's waveforms. Square and sawtooth are band-limited with PolyBLEP, as the browser's are. */
    private fun oscillator(wave: Wave, phase: Double, step: Double): Double = when (wave) {
        Wave.Sine -> sin(2 * PI * phase)
        Wave.Triangle -> when {
            phase < 0.25 -> 4 * phase
            phase < 0.75 -> 2 - 4 * phase
            else -> 4 * phase - 4
        }
        Wave.Sawtooth -> {
            val p = (phase + 0.5) % 1.0
            2 * p - 1 - polyBlep(p, step)
        }
        Wave.Square -> {
            val naive = if (phase < 0.5) 1.0 else -1.0
            naive + polyBlep(phase, step) - polyBlep((phase + 0.5) % 1.0, step)
        }
    }

    private fun polyBlep(t: Double, dt: Double): Double = when {
        t < dt -> {
            val x = t / dt
            x + x - x * x - 1
        }
        t > 1 - dt -> {
            val x = (t - 1) / dt
            x * x + x + x + 1
        }
        else -> 0.0
    }

    private fun noiseBuffer(color: Noise, frames: Int, random: Random): DoubleArray {
        val buffer = DoubleArray(frames)
        when (color) {
            Noise.White -> for (i in buffer.indices) buffer[i] = random.nextDouble() * 2 - 1
            Noise.Pink -> {
                // The kit's 3-pole pink approximation.
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                for (i in buffer.indices) {
                    val w = random.nextDouble() * 2 - 1
                    b0 = 0.99765 * b0 + w * 0.099046
                    b1 = 0.963 * b1 + w * 0.2965164
                    b2 = 0.57 * b2 + w * 1.0526913
                    buffer[i] = (b0 + b1 + b2 + w * 0.1848) * 0.11
                }
            }
        }
        return buffer
    }

    /**
     * BiquadFilterNode coefficients from the Web Audio spec. Note the spec reads a low-pass
     * Q in dB but a band-pass Q as a plain ratio.
     */
    private class Biquad(filter: Filter, sampleRate: Int) {
        // Coefficients, already divided by a0.
        private val b0: Double
        private val b1: Double
        private val b2: Double
        private val a1: Double
        private val a2: Double
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        init {
            val w0 = 2 * PI * filter.frequency / sampleRate
            val cosW = cos(w0)
            val sinW = sin(w0)
            val alpha = when (filter.type) {
                FilterType.LowPass -> sinW / (2 * 10.0.pow(filter.q / 20))
                FilterType.BandPass -> sinW / (2 * filter.q)
            }
            val a0 = 1 + alpha
            when (filter.type) {
                FilterType.LowPass -> {
                    b0 = (1 - cosW) / 2 / a0
                    b1 = (1 - cosW) / a0
                    b2 = b0
                }
                FilterType.BandPass -> {
                    b0 = alpha / a0
                    b1 = 0.0
                    b2 = -alpha / a0
                }
            }
            a1 = -2 * cosW / a0
            a2 = (1 - alpha) / a0
        }

        fun process(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }
    }
}
