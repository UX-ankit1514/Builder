package com.uxankit.stepwise.sound

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferInterrupts
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive

/**
 * Renders every UI sound once into memory and plays them through a few AVAudioPlayerNodes.
 * The ambient session category mixes with other audio and follows the silent switch.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosUiSounds : UiSoundPlayer {
    private val engine = AVAudioEngine()
    private val format = AVAudioFormat(standardFormatWithSampleRate = SoundSynth.SAMPLE_RATE.toDouble(), channels = 1u)
    private val voices = List(VOICES) { AVAudioPlayerNode() }
    private var nextVoice = 0
    private val buffers: Map<UiSound, AVAudioPCMBuffer> = UiSound.entries.associateWith { buffer(SoundSynth.render(it)) }

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, error = null)
        session.setActive(true, error = null)
        voices.forEach { voice ->
            engine.attachNode(voice)
            engine.connect(voice, to = engine.mainMixerNode, format = format)
        }
    }

    override fun play(sound: UiSound) {
        val buffer = buffers[sound] ?: return
        // A phone call or route change stops the engine; start it again on the next sound.
        if (!engine.running && !engine.startAndReturnError(null)) return
        if (sound.cutsOff) voices.forEach { it.stop() }
        val voice = voices[nextVoice]
        nextVoice = (nextVoice + 1) % voices.size
        voice.stop()
        voice.scheduleBuffer(buffer, atTime = null, options = AVAudioPlayerNodeBufferInterrupts, completionHandler = null)
        voice.play()
    }

    private fun buffer(samples: FloatArray): AVAudioPCMBuffer {
        val pcm = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = samples.size.toUInt())
        pcm.frameLength = samples.size.toUInt()
        val channel = pcm.floatChannelData!![0]!!
        samples.forEachIndexed { i, value -> channel[i] = value }
        return pcm
    }

    private companion object {
        const val VOICES = 6
    }
}
