package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class FocusAudioSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val sampleRate = 44100
    private var currentSoundType = "none"

    fun start(soundType: String) {
        stop()
        currentSoundType = soundType
        if (soundType == "none") return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioTrack?.play()

        job = CoroutineScope(Dispatchers.Default).launch {
            val buffer = ShortArray(bufferSize)
            var phaseLeft = 0.0
            var phaseRight = 0.0
            var wavePhase = 0.0
            val random = Random(System.currentTimeMillis())

            while (isActive) {
                for (i in buffer.indices step 2) {
                    if (!isActive) break
                    when (soundType) {
                        "white_noise" -> {
                            val sample = (random.nextFloat() * 2f - 1f) * 0.15f
                            val value = (sample * Short.MAX_VALUE).toInt().toShort()
                            buffer[i] = value
                            buffer[i + 1] = value
                        }
                        "ocean" -> {
                            wavePhase += 2.0 * Math.PI / (sampleRate * 4) // 4 second wave cycle
                            val volumeMod = (sin(wavePhase) + 1.0) / 2.0 * 0.12 + 0.03
                            val sample = (random.nextFloat() * 2f - 1f) * volumeMod.toFloat()
                            val value = (sample * Short.MAX_VALUE).toInt().toShort()
                            buffer[i] = value
                            buffer[i + 1] = value
                        }
                        "binaural" -> {
                            val freqL = 200.0
                            val freqR = 210.0
                            phaseLeft += 2.0 * Math.PI * freqL / sampleRate
                            phaseRight += 2.0 * Math.PI * freqR / sampleRate
                            
                            val sampleL = sin(phaseLeft) * 0.20
                            val sampleR = sin(phaseRight) * 0.20
                            
                            buffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                            buffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                        }
                        "space" -> {
                            val freqL = 80.0
                            val freqR = 82.0
                            phaseLeft += 2.0 * Math.PI * freqL / sampleRate
                            phaseRight += 2.0 * Math.PI * freqR / sampleRate
                            
                            wavePhase += 2.0 * Math.PI * 0.15 / sampleRate
                            val lfo = sin(wavePhase) * 0.04
                            
                            val sampleL = (sin(phaseLeft) + sin(phaseLeft * 1.5)) * (0.12 + lfo)
                            val sampleR = (sin(phaseRight) + sin(phaseRight * 1.5)) * (0.12 + lfo)
                            
                            buffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                            buffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
        currentSoundType = "none"
    }

    fun getCurrentSoundType(): String = currentSoundType
}
