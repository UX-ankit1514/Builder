package com.uxankit.stepwise.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.uxankit.stepwise.platform.LocalPlatform
import com.uxankit.stepwise.ui.theme.StepwiseTheme

/**
 * Plays a rendered [UiSound]. Each platform renders every recipe once with [SoundSynth],
 * keeps it in memory and stays quiet when the phone is on silent.
 */
fun interface UiSoundPlayer {
    fun play(sound: UiSound)

    companion object {
        val None = UiSoundPlayer { }
    }
}

/** What screens call to play a sound. Silent while Reduce motion is on, as the kit is under prefers-reduced-motion. */
@Stable
class UiSounds internal constructor(private val player: UiSoundPlayer, private val quiet: Boolean) {
    fun play(sound: UiSound?) {
        if (sound != null && !quiet) player.play(sound)
    }
}

@Composable
fun rememberUiSounds(): UiSounds {
    val player = LocalPlatform.current.sounds
    val quiet = StepwiseTheme.settings.reduceMotion
    return remember(player, quiet) { UiSounds(player, quiet) }
}
