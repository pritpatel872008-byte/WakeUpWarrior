package com.wakeupwarrior.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            var selected by remember { mutableStateOf("Home") }
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bedtime, null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                    Column {
                        Text("WakeUp Warrior", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Wake up. Show up. Win your day.", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
                    }
                }
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("NEXT ALARM", color = Mint, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                        Text("07:00 AM", fontSize = 48.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Your morning starts with a win.", color = Ink.copy(alpha = .65f))
                        Button(onClick = { selected = "Alarm setup" }, shape = RoundedCornerShape(18.dp)) {
                            Icon(Icons.Default.Alarm, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Set an alarm")
                        }
                    }
                }
                Text("YOUR CHALLENGES", fontWeight = FontWeight.Bold, color = Ink, letterSpacing = 1.sp)
                ChallengeTile("Quick Quiz", "Answer offline questions", Icons.Default.Quiz) { selected = "Quick Quiz" }
                ChallengeTile("Dance Challenge", "Move to wake your body", Icons.Default.DirectionsRun) { selected = "Dance Challenge" }
                ChallengeTile("Brush Challenge", "Complete your 2-minute timer", Icons.Default.SelfImprovement) { selected = "Brush Challenge" }
                ChallengeTile("Lucky Wheel", "Find a household object", Icons.Default.Alarm) { selected = "Lucky Wheel" }
                Spacer(Modifier.weight(1f))
                Text(selected, color = Mint, fontWeight = FontWeight.Bold)
                Text("Foundation screen only — challenge and alarm engines are being implemented next.", color = Ink.copy(alpha = .6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChallengeTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFE5F5EF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Mint)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = .62f))
            }
        }
    }
}
