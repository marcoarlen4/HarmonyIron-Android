package com.harmonyiron.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.harmonyiron.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 组间休息倒计时前台服务：提供锁屏常驻实时倒计时通知与到点三段式震动提醒。
 */
class RestTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private var endTimeMillis: Long = 0L
    private var currentExerciseName: String = "组间休息"

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 90)
                currentExerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "组间休息"
                startCountdown(seconds)
            }
            ACTION_ADD_30S -> {
                if (endTimeMillis > System.currentTimeMillis()) {
                    val remainingSec = ((endTimeMillis - System.currentTimeMillis()) / 1000L).toInt() + 30
                    startCountdown(remainingSec)
                }
            }
            ACTION_SKIP -> {
                stopCountdown()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(seconds: Int) {
        timerJob?.cancel()
        val safeSeconds = seconds.coerceAtLeast(1)
        endTimeMillis = System.currentTimeMillis() + safeSeconds * 1000L
        _remainingSeconds.value = safeSeconds
        _isRunning.value = true

        startForeground(NOTIFICATION_ID, buildNotification(endTimeMillis))

        timerJob = serviceScope.launch {
            while (true) {
                val left = ((endTimeMillis - System.currentTimeMillis() + 999L) / 1000L).toInt()
                if (left <= 0) {
                    _remainingSeconds.value = 0
                    _isRunning.value = false
                    triggerFinishVibration()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                _remainingSeconds.value = left
                delay(500L)
            }
        }
    }

    private fun stopCountdown() {
        timerJob?.cancel()
        _remainingSeconds.value = 0
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(targetEndMillis: Long): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add30sIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_ADD_30S },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("鸿铁 · $currentExerciseName")
            .setContentText("组间休息中，准备好下一组")
            .setWhen(targetEndMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .addAction(0, "+30秒", add30sIntent)
            .addAction(0, "跳过", skipIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "组间休息倒计时",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在锁屏与通知栏实时显示组间休息倒计时"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun triggerFinishVibration() {
        val pattern = longArrayOf(0, 220, 120, 220, 120, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "harmony_iron_rest_timer"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.harmonyiron.app.action.START_REST_TIMER"
        const val ACTION_ADD_30S = "com.harmonyiron.app.action.ADD_30S"
        const val ACTION_SKIP = "com.harmonyiron.app.action.SKIP_REST_TIMER"
        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_EXERCISE_NAME = "extra_exercise_name"

        private val _remainingSeconds = MutableStateFlow(0)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, seconds: Int, exerciseName: String) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
            }
            context.startForegroundService(intent)
        }

        fun skip(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_SKIP
            }
            context.startService(intent)
        }
    }
}
