package com.example.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundAndVibrationHelper(private val context: Context) {

    fun playRestCompletedCue(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            playBeep(ToneGenerator.TONE_PROP_BEEP2, 320)
        }
        if (vibrationEnabled) {
            vibratePattern(longArrayOf(0, 140, 80, 220))
        }
    }

    fun playSetLoggedCue(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            playBeep(ToneGenerator.TONE_PROP_ACK, 120)
        }
        if (vibrationEnabled) {
            vibratePattern(longArrayOf(0, 50))
        }
    }

    fun playTempoTick(soundEnabled: Boolean) {
        if (soundEnabled) {
            playBeep(ToneGenerator.TONE_PROP_BEEP, 60)
        }
    }

    private fun playBeep(toneType: Int, durationMs: Int) {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(toneType, durationMs)
            // Release after brief delay on background thread to avoid cutting off tone
            Thread {
                runCatching {
                    Thread.sleep(durationMs.toLong() + 80L)
                    toneGen.release()
                }
            }.start()
        }
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(timings: LongArray) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    vibrator.vibrate(timings, -1)
                }
            }
        }
    }
}
