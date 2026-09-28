package com.uxankit.stepwise.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Renders every UI sound once into a small WAV in the cache (nothing ships in the APK), then plays
 * them through SoundPool: low latency, several at once. Silent or vibrate mode keeps the app quiet,
 * and the level follows the phone's system sounds volume.
 */
internal class AndroidUiSounds private constructor(context: Context) : UiSoundPlayer {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val sampleIds = ConcurrentHashMap<UiSound, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()

    /** Streams started recently, so a navigation sound can cut them off. Main thread only. */
    private val ringing = ArrayDeque<Pair<Int, Long>>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
        thread(name = "ui-sounds", isDaemon = true) {
            val dir = File(context.cacheDir, "ui-sounds").apply { mkdirs() }
            UiSound.entries.forEach { sound ->
                val file = File(dir, "${sound.name}.wav")
                file.writeWav(SoundSynth.render(sound), SoundSynth.SAMPLE_RATE)
                sampleIds[sound] = pool.load(file.path, 1)
            }
        }
    }

    override fun play(sound: UiSound) {
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val sampleId = sampleIds[sound]?.takeIf { it in loaded } ?: return
        val now = SystemClock.uptimeMillis()
        while (ringing.isNotEmpty() && now - ringing.first().second > RING_MS) ringing.removeFirst()
        if (sound.cutsOff) {
            ringing.forEach { (stream, _) -> pool.stop(stream) }
            ringing.clear()
        }
        val stream = pool.play(sampleId, 1f, 1f, 1, 0, 1f)
        if (stream != 0) ringing.addLast(stream to now)
    }

    companion object {
        /** Longest sound (success) plus a margin. */
        private const val RING_MS = 700L

        @Volatile
        private var instance: AndroidUiSounds? = null

        /** One player for the whole process, so a rotated Activity doesn't open a second SoundPool. */
        fun get(context: Context): AndroidUiSounds =
            instance ?: synchronized(this) {
                instance ?: AndroidUiSounds(context.applicationContext).also { instance = it }
            }
    }
}

/** 16-bit mono PCM WAV. */
private fun File.writeWav(samples: FloatArray, sampleRate: Int) {
    val dataBytes = samples.size * 2
    val wav = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
    wav.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
    wav.put("fmt ".toByteArray()).putInt(16)
        .putShort(1) // PCM
        .putShort(1) // mono
        .putInt(sampleRate)
        .putInt(sampleRate * 2) // byte rate
        .putShort(2) // block align
        .putShort(16) // bits per sample
    wav.put("data".toByteArray()).putInt(dataBytes)
    samples.forEach { wav.putShort((it * Short.MAX_VALUE).roundToInt().toShort()) }
    writeBytes(wav.array())
}
