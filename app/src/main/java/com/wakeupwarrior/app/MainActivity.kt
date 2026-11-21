package com.wakeupwarrior.app

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
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

private val Mint = Color(0xFF62BFA6)
private val Ink = Color(0xFF23443D)
private val Canvas = Color(0xFFF3FAF7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WakeUpWarriorApp() }
    }
}

@Composable
fun WakeUpWarriorApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Mint, onPrimary = Color.White, background = Canvas, surface = Color.White)) {
        var screen by remember { mutableStateOf("Home") }
        Surface(Modifier.fillMaxSize(), color = Canvas) {
            when (screen) {
                "Alarm setup" -> AlarmSetupScreen(onBack = { screen = "Home" })
                else -> HomeScreen(onSetAlarm = { screen = "Alarm setup" })
            }
        }
    }
}

@Composable
private fun HomeScreen(onSetAlarm: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bedtime, null, tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Column {
                Text("WakeUp Warrior", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Wake up. Show up. Win your day.", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NEXT ALARM", color = Mint, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 11.sp)
                Text("--:--", fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("No alarm scheduled yet", color = Ink.copy(alpha = .65f), fontSize = 13.sp)
                Button(onClick = onSetAlarm, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Alarm, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set Alarm", fontSize = 16.sp)
                }
            }
        }
        Text("YOUR CHALLENGES", fontWeight = FontWeight.Bold, color = Ink, letterSpacing = 1.sp, fontSize = 12.sp)
        ChallengeTile("Quick Quiz", "Offline questions", Icons.Default.Quiz)
        ChallengeTile("Dance Challenge", "Movement challenge", Icons.Default.DirectionsRun)
        ChallengeTile("Brush Challenge", "2-minute timer", Icons.Default.SelfImprovement)
        ChallengeTile("Lucky Wheel", "Household object challenge", Icons.Default.Alarm)
        Text("Alarm and challenge engines are still under development.", color = Ink.copy(alpha = .58f), fontSize = 11.sp)
    }
}

@Composable
private fun AlarmSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var repeatDays by remember { mutableStateOf(setOf("M", "T", "W", "T", "F")) }
    var quiz by remember { mutableStateOf(true) }
    var dance by remember { mutableStateOf(false) }
    var brush by remember { mutableStateOf(false) }
    var lucky by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Set your alarm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WAKE-UP TIME", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(
                    String.format("%02d:%02d", if (hour % 12 == 0) 12 else hour % 12, minute) + if (hour < 12) " AM" else " PM",
                    fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Ink,
                    modifier = Modifier.clickable {
                        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
                    }
                )
                OutlinedButton(
                    onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show() },
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) { Text("Choose time") }
            }
        }
        Text("REPEAT", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
                val key = "$day$index"
                val selected = repeatDays.contains(key)
                Box(
                    Modifier.size(38.dp).background(if (selected) Mint else Color.White, CircleShape)
                        .clickable { repeatDays = if (selected) repeatDays - key else repeatDays + key },
                    contentAlignment = Alignment.Center
                ) { Text(day, color = if (selected) Color.White else Ink, fontWeight = FontWeight.SemiBold) }
            }
        }
        Text("WAKE-UP CHALLENGES", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        ChallengeToggle("Quick Quiz", "Answer questions to dismiss", Icons.Default.Quiz, quiz) { quiz = it }
        ChallengeToggle("Dance Challenge", "Camera is used only when you start it", Icons.Default.DirectionsRun, dance) { dance = it }
        ChallengeToggle("Brush Challenge", "2-minute brushing timer", Icons.Default.SelfImprovement, brush) { brush = it }
        ChallengeToggle("Lucky Wheel", "Camera is used only when you start it", Icons.Default.Alarm, lucky) { lucky = it }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { showInfo = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Save alarm setup", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Text("Alarm scheduling and ringtone saving are being implemented next.", color = Ink.copy(alpha = .58f), fontSize = 10.sp)
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Setup preview") },
            text = { Text("Your time and challenge selections are currently a screen preview. The alarm scheduler and saving will be connected in the next implementation step.") },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } }
        )
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

@Composable
private fun ChallengeTile(title: String, subtitle: String, icon: ImageVector) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(0xFFE5F5EF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Mint)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(subtitle, fontSize = 11.sp, color = Ink.copy(alpha = .62f))
            }
        }
    }
}
