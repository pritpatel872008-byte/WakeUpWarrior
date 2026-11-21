package com.wakeupwarrior.app

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val Mint = Color(0xFF62BFA6)
private val Ink = Color(0xFF23443D)
private val Canvas = Color(0xFFF3FAF7)
private val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
private val dayValues = listOf(
    Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WakeUpWarriorApp() }
    }
}

@Composable
fun WakeUpWarriorApp() {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(AlarmStore.all(context)) }
    var editing by remember { mutableStateOf<AlarmItem?>(null) }
    var showSetup by remember { mutableStateOf(false) }

    fun refresh() { alarms = AlarmStore.all(context) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Mint, onPrimary = Color.White, background = Canvas, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Canvas) {
            if (showSetup) {
                AlarmSetupScreen(
                    initial = editing,
                    onBack = { showSetup = false; editing = null },
                    onSave = { item ->
                        AlarmStore.save(context, item)
                        AlarmScheduler.schedule(context, item)
                        refresh()
                        showSetup = false
                        editing = null
                    }
                )
            } else {
                HomeScreen(
                    alarms = alarms,
                    onAdd = { editing = null; showSetup = true },
                    onEdit = { editing = it; showSetup = true },
                    onDelete = { AlarmScheduler.cancel(context, it); AlarmStore.delete(context, it); refresh() },
                    onToggle = { id, enabled ->
                        AlarmStore.setEnabled(context, id, enabled)
                        if (enabled) AlarmStore.all(context).firstOrNull { it.id == id }?.let { AlarmScheduler.schedule(context, it) }
                        else AlarmScheduler.cancel(context, id)
                        refresh()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    alarms: List<AlarmItem>,
    onAdd: () -> Unit,
    onEdit: (AlarmItem) -> Unit,
    onDelete: (Int) -> Unit,
    onToggle: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bedtime, null, tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Column {
                Text("WakeUp Warrior", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Wake up. Show up. Win your day.", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Alarm, null)
            Spacer(Modifier.width(8.dp))
            Text("Add alarm", fontSize = 16.sp)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(android.app.AlarmManager::class.java)
            if (!manager.canScheduleExactAlarms()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow precise alarm timing") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("YOUR ALARMS", fontWeight = FontWeight.Bold, color = Ink, letterSpacing = 1.sp, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (alarms.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No alarms yet", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Every alarm includes all four wake-up challenges.", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clickable { onEdit(alarm) }) {
                                    Text(formatTime(alarm.hour, alarm.minute), fontSize = 30.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text(repeatLabel(alarm.repeatDays), fontSize = 11.sp, color = Ink.copy(alpha = .65f))
                                }
                                Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
                                IconButton(onClick = { onEdit(alarm) }) { Icon(Icons.Default.Edit, "Edit alarm", tint = Ink) }
                                IconButton(onClick = { onDelete(alarm.id) }) { Icon(Icons.Default.Delete, "Delete alarm", tint = Color(0xFFB34C59)) }
                            }
                            val selected = listOfNotNull(
                                if (alarm.quiz) "Quiz" else null,
                                if (alarm.dance) "Dance" else null,
                                if (alarm.brush) "Brush" else null,
                                if (alarm.lucky) "Lucky Wheel" else null
                            )
                            Text(if (selected.isEmpty()) "No challenges selected" else selected.joinToString(" • "), color = Ink.copy(alpha = .7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSetupScreen(initial: AlarmItem?, onBack: () -> Unit, onSave: (AlarmItem) -> Unit) {
    val context = LocalContext.current
    var hour by remember(initial?.id) { mutableIntStateOf(initial?.hour ?: 7) }
    var minute by remember(initial?.id) { mutableIntStateOf(initial?.minute ?: 0) }
    var repeatDays by remember(initial?.id) { mutableStateOf(initial?.repeatDays ?: emptySet()) }
    var ringtone by remember(initial?.id) { mutableStateOf(initial?.ringtone) }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val picked = if (Build.VERSION.SDK_INT >= 33) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            ringtone = picked?.toString()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = Ink) }
            Text(if (initial == null) "Set your alarm" else "Edit alarm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAKE-UP TIME", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(formatTime(hour, minute), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Ink)
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
                    }) { Text("Choose time") }
                }
            }
            Text("REPEAT  •  leave all days off for a one-time alarm", color = Ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayLabels.forEachIndexed { index, label ->
                    val day = dayValues[index]
                    val selected = day in repeatDays
                    Box(
                        Modifier.size(38.dp).background(if (selected) Mint else Color.White, CircleShape)
                            .clickable { repeatDays = if (selected) repeatDays - day else repeatDays + day },
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (selected) Color.White else Ink, fontWeight = FontWeight.SemiBold) }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F5EF))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ALL 4 CHALLENGES INCLUDED", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Quick Quiz • Dance • Brush • Lucky Wheel", color = Ink, fontSize = 12.sp)
                    Text("No manual selection. Complete every task to dismiss the alarm.", color = Ink.copy(alpha = .7f), fontSize = 10.sp)
                }
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        ringtone?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                    }
                    ringtonePicker.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MusicNote, null)
                Spacer(Modifier.width(8.dp))
                Text(if (ringtone == null) "Choose alarm ringtone" else "Change alarm ringtone")
            }
            Text(if (ringtone == null) "Default alarm sound will be used." else "Custom alarm sound selected.", color = Ink.copy(alpha = .6f), fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val id = initial?.id ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                onSave(AlarmItem(id, hour, minute, repeatDays, ringtone, true, true, true, true, true))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Save alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ChallengeToggle(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(Color(0xFFE5F5EF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Mint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 13.sp)
                Text(subtitle, color = Ink.copy(alpha = .62f), fontSize = 10.sp, lineHeight = 12.sp)
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return String.format("%02d:%02d %s", h, minute, if (hour < 12) "AM" else "PM")
}

private fun repeatLabel(days: Set<Int>): String {
    if (days.isEmpty()) return "One time"
    val names = mapOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat")
    return days.sorted().joinToString(" • ") { names[it] ?: "" }
}
