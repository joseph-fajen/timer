package com.igygtimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.igygtimer.IGYGApplication
import com.igygtimer.MainActivity
import com.igygtimer.R
import com.igygtimer.audio.BeepPlayer
import com.igygtimer.model.TimerPhase
import com.igygtimer.repository.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
        private const val WAKELOCK_TAG = "IGYGTimer::TimerWakeLock"

        const val ACTION_START = "com.igygtimer.action.START"
        const val ACTION_STOP = "com.igygtimer.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var repository: TimerRepository
    private lateinit var beepPlayer: BeepPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        repository = (application as IGYGApplication).container.timerRepository

        beepPlayer = BeepPlayer(this)
        repository.beepPlayer = beepPlayer

        createNotificationChannel()

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startTickLoop()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.beepPlayer = null
        beepPlayer.release()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "IGYG Timer workout notifications"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Starting workout...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IGYG Timer")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startTickLoop() {
        var lastNotificationSecond = -1L

        lifecycleScope.launch {
            while (isActive) {
                repository.tick()

                val state = repository.uiState.value
                val currentSecond = state.displayTimeMs / 1000

                // Only update notification once per second to avoid rate limiting
                if (currentSecond != lastNotificationSecond) {
                    lastNotificationSecond = currentSecond

                    val notificationText = when (state.phase) {
                        is TimerPhase.Work -> {
                            "WORK - Round ${state.currentRound}/${state.totalRounds} - ${formatTime(currentSecond)}"
                        }
                        is TimerPhase.Rest -> {
                            "REST - Round ${state.currentRound}/${state.totalRounds} - ${formatTime(currentSecond)}"
                        }
                        is TimerPhase.Paused -> "PAUSED - Round ${state.currentRound}/${state.totalRounds}"
                        is TimerPhase.Complete -> {
                            stopSelf()
                            "Complete!"
                        }
                        is TimerPhase.Idle -> {
                            stopSelf()
                            "Idle"
                        }
                    }
                    updateNotification(notificationText)
                }

                if (!repository.isActive()) {
                    stopSelf()
                    break
                }

                delay(50)
            }
        }
    }

    private fun formatTime(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
