package com.wakeupwarrior.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmSoundService : Service() {
    private var player: MediaPlayer? = null
    private var currentAlarmId: Int = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        currentAlarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wake_up_warrior)
            .setContentTitle("WakeUp Warrior is ringing")
            .setContentText("Complete your wake-up tasks.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        startPlayback(intent?.getStringExtra("ringtone"))
        return START_STICKY
    }

    private fun startPlayback(ringtoneText: String?) {
        stopPlayback()
        val alarmUri = ringtoneText?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setOnErrorListener { mp, _, _ ->
                mp.reset()
                runCatching {
                    setDataSource(this@AlarmSoundService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    isLooping = true
                    setVolume(1f, 1f)
                    prepare()
                    start()
                }
                true
            }
            runCatching {
                setDataSource(this@AlarmSoundService, alarmUri)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }.onFailure {
                runCatching {
                    reset()
                    setDataSource(this@AlarmSoundService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    isLooping = true
                    setVolume(1f, 1f)
                    prepare()
                    start()
                }
            }
        }
    }

    private fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Alarm sound", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.wakeupwarrior.app.STOP_ALARM_SOUND"
        private const val CHANNEL_ID = "wake_alarm_sound"
        private const val NOTIFICATION_ID = 7401

        fun stop(context: Context) {
            context.startService(Intent(context, AlarmSoundService::class.java).setAction(ACTION_STOP))
        }
    }
}
