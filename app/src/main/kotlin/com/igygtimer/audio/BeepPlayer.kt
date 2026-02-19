package com.igygtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.igygtimer.R

class BeepPlayer(context: Context) {

    private val soundPool: SoundPool
    private var beepSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            isLoaded = (status == 0)
        }

        beepSoundId = soundPool.load(context, R.raw.beep, 1)
    }

    fun playBeep() {
        if (isLoaded && beepSoundId != 0) {
            soundPool.play(
                beepSoundId,
                1.0f,
                1.0f,
                1,
                0,
                1.0f
            )
        }
    }

    fun release() {
        soundPool.release()
    }
}
