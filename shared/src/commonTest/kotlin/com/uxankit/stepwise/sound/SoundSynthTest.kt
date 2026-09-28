package com.uxankit.stepwise.sound

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class SoundSynthTest {
    private val rate = SoundSynth.SAMPLE_RATE

    @Test
    fun everySoundIsAudibleFiniteAndInRange() {
        UiSound.entries.forEach { sound ->
            val samples = SoundSynth.render(sound)
            assertTrue(samples.all { it.isFinite() && it in -1f..1f }, "$sound has samples out of range")
            val peak = samples.maxOf { abs(it) }
            assertTrue(peak > 0.05f, "$sound is too quiet: peak $peak")
        }
    }

    @Test
    fun lengthIsTheLongestLayerPlusTheTail() {
        UiSound.entries.forEach { sound ->
            val expected = sound.layers.maxOf { it.delay + it.length + 0.1 }
            val actual = SoundSynth.render(sound).size.toDouble() / rate
            assertTrue(abs(actual - expected) < 0.001, "$sound lasts $actual s, expected $expected s")
        }
    }

    @Test
    fun soundsFadeOutBeforeTheyEnd() {
        UiSound.entries.forEach { sound ->
            val samples = SoundSynth.render(sound)
            val lastTenMs = samples.takeLast(rate / 100).maxOf { abs(it) }
            assertTrue(lastTenMs < 0.01f, "$sound still rings at the end: $lastTenMs")
        }
    }

    @Test
    fun delayedLayersStartLate() {
        // Save: the first blip has died away by 45ms; the second blip starts at 50ms.
        val save = SoundSynth.render(UiSound.Save)
        fun peak(from: Double, to: Double) = save.copyOfRange((from * rate).toInt(), (to * rate).toInt()).maxOf { abs(it) }
        assertTrue(peak(0.0, 0.01) > 0.3f)
        assertTrue(peak(0.045, 0.0499) < 0.01f)
        assertTrue(peak(0.05, 0.06) > 0.3f)
    }

    @Test
    fun renderingIsRepeatable() {
        UiSound.entries.forEach { assertContentEquals(SoundSynth.render(it), SoundSynth.render(it), "$it") }
    }
}
