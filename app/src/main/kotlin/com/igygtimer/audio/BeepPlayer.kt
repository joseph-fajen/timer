package com.igygtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.igygtimer.R

class BeepPlayer(context: Context) {

    companion object {
        private const val TAG = "BeepPlayer"
    }

    private val soundPool: SoundPool
    private var beepSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            isLoaded = (status == 0)
            Log.d(TAG, "Sound loaded: sampleId=$sampleId, status=$status, isLoaded=$isLoaded")
        }

        beepSoundId = soundPool.load(context, R.raw.beep, 1)
        Log.d(TAG, "Loading sound, beepSoundId=$beepSoundId")
    }

    fun playBeep() {
        Log.d(TAG, "playBeep called: isLoaded=$isLoaded, beepSoundId=$beepSoundId")
        if (isLoaded && beepSoundId != 0) {
            val streamId = soundPool.play(
                beepSoundId,
                1.0f,
                1.0f,
                1,
                0,
                1.0f
            )
            Log.d(TAG, "Sound played, streamId=$streamId")
        } else {
            Log.w(TAG, "Cannot play: sound not loaded")
        }
    }

    fun release() {
        soundPool.release()
    }
}
