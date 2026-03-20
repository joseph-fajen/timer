package com.igygtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.igygtimer.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BeepPlayer(context: Context) {

    companion object {
        private const val TAG = "BeepPlayer"
        private const val LOAD_TIMEOUT_MS = 5000L
    }

    private val soundPool: SoundPool
    private var beepSoundId: Int = 0
    private val loadLatch = CountDownLatch(1)

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
            Log.d(TAG, "Sound loaded: sampleId=$sampleId, status=$status")
            if (status == 0) {
                loadLatch.countDown()
            }
        }

        beepSoundId = soundPool.load(context, R.raw.beep, 1)
        Log.d(TAG, "Loading sound, beepSoundId=$beepSoundId")
    }

    fun playBeep() {
        Log.d(TAG, "playBeep called: beepSoundId=$beepSoundId")
        if (beepSoundId == 0) {
            Log.w(TAG, "Cannot play: no sound ID")
            return
        }
        if (!loadLatch.await(LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Log.w(TAG, "Cannot play: sound load timed out")
            return
        }
        val streamId = soundPool.play(
            beepSoundId,
            1.0f,
            1.0f,
            1,
            0,
            1.0f
        )
        Log.d(TAG, "Sound played, streamId=$streamId")
    }

    fun release() {
        soundPool.release()
    }
}
