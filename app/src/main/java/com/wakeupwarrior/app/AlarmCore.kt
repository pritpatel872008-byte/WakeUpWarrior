package com.wakeupwarrior.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<Int>,
    val ringtone: String?,
    val quiz: Boolean = false,
    val dance: Boolean = false,
    val brush: Boolean = false,
    val lucky: Boolean = false,
    val enabled: Boolean = true
)

object AlarmStore {
    private const val PREFS = "wake_up_warrior_alarms"
    private const val KEY = "items"

    fun all(context: Context): List<AlarmItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val days = o.getJSONArray("days")
                AlarmItem(
                    id = o.getInt("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    repeatDays = (0 until days.length()).map { days.getInt(it) }.toSet(),
                    ringtone = o.optString("ringtone").takeIf { it.isNotBlank() && it != "null" },
                    quiz = o.optBoolean("quiz"),
                    dance = o.optBoolean("dance"),
                    brush = o.optBoolean("brush"),
                    lucky = o.optBoolean("lucky"),
                    enabled = o.optBoolean("enabled", true)
                )
            }.getOrNull()
        }
    }

    fun save(context: Context, item: AlarmItem) {
        val items = all(context).filterNot { it.id == item.id } + item
        persist(context, items)
    }

    fun delete(context: Context, id: Int) {
        persist(context, all(context).filterNot { it.id == id })
    }

    fun setEnabled(context: Context, id: Int, enabled: Boolean) {
        persist(context, all(context).map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    private fun persist(context: Context, items: List<AlarmItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val days = JSONArray()
            item.repeatDays.sorted().forEach(days::put)
            array.put(JSONObject().apply {
                put("id", item.id); put("hour", item.hour); put("minute", item.minute)
                put("days", days); put("ringtone", item.ringtone)
                put("quiz", item.quiz); put("dance", item.dance); put("brush", item.brush)
                put("lucky", item.lucky); put("enabled", item.enabled)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}

object AlarmScheduler {
    fun schedule(context: Context, item: AlarmItem) {
        if (!item.enabled) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTrigger(item)
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("alarm_id", item.id)
        val pending = PendingIntent.getBroadcast(
            context, item.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    fun cancel(context: Context, id: Int) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
    }

    fun rescheduleAll(context: Context) {
        AlarmStore.all(context).forEach { if (it.enabled) schedule(context, it) }
    }

    private fun nextTrigger(item: AlarmItem): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hour)
            set(Calendar.MINUTE, item.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (item.repeatDays.isEmpty()) {
            if (!candidate.after(now)) candidate.add(Calendar.DAY_OF_YEAR, 1)
            return candidate.timeInMillis
        }
        for (offset in 0..7) {
            val day = (now.get(Calendar.DAY_OF_WEEK) - 1 + offset) % 7 + 1
            candidate.timeInMillis = now.timeInMillis
            candidate.add(Calendar.DAY_OF_YEAR, offset)
            candidate.set(Calendar.HOUR_OF_DAY, item.hour)
            candidate.set(Calendar.MINUTE, item.minute)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)
            if (day in item.repeatDays && candidate.after(now)) return candidate.timeInMillis
        }
        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("alarm_id", -1)
        val item = AlarmStore.all(context).firstOrNull { it.id == id } ?: return
        if (!item.enabled) return

        if (item.repeatDays.isEmpty()) {
            AlarmStore.setEnabled(context, id, false)
        } else {
            AlarmScheduler.schedule(context, item)
        }

        val soundIntent = Intent(context, AlarmSoundService::class.java)
            .putExtra("alarm_id", id)
            .putExtra("ringtone", item.ringtone)
        androidx.core.content.ContextCompat.startForegroundService(context, soundIntent)

        val fullScreen = Intent(context, AlarmRingActivity::class.java)
            .putExtra("alarm_id", id)
            .putExtra("ringtone", item.ringtone)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, id, fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(Context.ALARM_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                android.app.NotificationChannel("wake_alarm", "Wake-up alarms", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        }
        val notification = androidx.core.app.NotificationCompat.Builder(context, "wake_alarm")
            .setSmallIcon(R.drawable.ic_wake_up_warrior)
            .setContentTitle("WakeUp Warrior")
            .setContentText("Your alarm is ringing. Time to get up!")
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_VIBRATE)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        manager.notify(id, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            AlarmScheduler.rescheduleAll(context)
        }
    }
}

