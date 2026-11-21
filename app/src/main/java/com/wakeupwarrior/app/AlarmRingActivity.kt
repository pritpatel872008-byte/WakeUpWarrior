package com.wakeupwarrior.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setContent {
            MaterialTheme {
                BackHandler(enabled = true) { /* Keep the alarm active until all challenges are complete. */ }
                AlarmChallengeFlow(
                    onAllTasksComplete = { stopAlarm() }
                )
            }
        }
    }

    private fun stopAlarm() {
        val id = intent.getIntExtra("alarm_id", -1)
        AlarmSoundService.stop(this)
        getSystemService(android.app.NotificationManager::class.java).cancel(id)
        finish()
    }
}

@Composable
private fun AlarmChallengeFlow(onAllTasksComplete: () -> Unit) {
    var taskIndex by remember { mutableIntStateOf(0) }
    var quizAnswered by remember { mutableStateOf(false) }
    var danceStarted by remember { mutableStateOf(false) }
    var danceSeconds by remember { mutableIntStateOf(20) }
    var brushStarted by remember { mutableStateOf(false) }
    var brushSeconds by remember { mutableIntStateOf(120) }
    var wheelResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(danceStarted, danceSeconds) {
        if (danceStarted && danceSeconds > 0) {
            delay(1000)
            danceSeconds--
        }
    }
    LaunchedEffect(brushStarted, brushSeconds) {
        if (brushStarted && brushSeconds > 0) {
            delay(1000)
            brushSeconds--
        }
    }

    val titles = listOf("Quick Quiz", "Dance Challenge", "Brush Challenge", "Lucky Wheel")
    val descriptions = listOf(
        "Answer the question correctly to continue.",
        "Start the 20-second dance challenge. Keep moving until the timer ends.",
        "Brush for the full two-minute timer before continuing.",
        "Spin the lucky wheel to unlock the final step."
    )

    Column(
        Modifier.fillMaxSize().background(Color(0xFF183E36)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WAKE UP WARRIOR", color = Color(0xFF9DE3CC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Challenge ${(taskIndex + 1).coerceAtMost(4)} of 4", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(Modifier.weight(1f).height(6.dp).background(if (index < taskIndex) Color(0xFF9DE3CC) else Color.White.copy(alpha = .25f), RoundedCornerShape(8.dp)))
            }
        }
        Spacer(Modifier.height(30.dp))

        if (taskIndex < 4) {
            Text(titles[taskIndex], color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(descriptions[taskIndex], color = Color.White.copy(alpha = .85f), fontSize = 15.sp)
            Spacer(Modifier.height(30.dp))

            when (taskIndex) {
                0 -> {
                    Text("What is 7 × 8?", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp))
                    listOf("54", "56", "64", "48").forEach { answer ->
                        OutlinedButton(
                            onClick = { if (answer == "56") quizAnswered = true },
                            enabled = !quizAnswered,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (quizAnswered && answer == "56") "56  ✓ Correct" else answer, color = Color.White) }
                    }
                    if (quizAnswered) {
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { taskIndex++ }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    } else {
                        Text("Choose the correct answer.", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
                    }
                }
                1 -> {
                    Text(if (danceSeconds > 0) "${danceSeconds}s" else "Challenge complete!", color = Color(0xFF9DE3CC), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    if (!danceStarted) {
                        Button(onClick = { danceStarted = true }, modifier = Modifier.fillMaxWidth()) { Text("Start dance challenge") }
                    } else if (danceSeconds == 0) {
                        Button(onClick = { taskIndex++ }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    } else {
                        Text("Keep moving until the timer reaches zero.", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    }
                }
                2 -> {
                    Text(if (brushSeconds > 0) String.format("%02d:%02d", brushSeconds / 60, brushSeconds % 60) else "Brushing time complete!", color = Color(0xFF9DE3CC), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    if (!brushStarted) {
                        Button(onClick = { brushStarted = true }, modifier = Modifier.fillMaxWidth()) { Text("Start 2-minute brush timer") }
                    } else if (brushSeconds == 0) {
                        Button(onClick = { taskIndex++ }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    } else {
                        Text("Keep brushing until the timer finishes.", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    }
                }
                3 -> {
                    Text(wheelResult ?: "Ready to spin?", color = Color(0xFF9DE3CC), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    if (wheelResult == null) {
                        Button(onClick = {
                            wheelResult = listOf("Today is your day!", "You are unstoppable!", "Great things await!", "Make it count!").random()
                        }, modifier = Modifier.fillMaxWidth()) { Text("SPIN THE LUCKY WHEEL") }
                    } else {
                        Button(onClick = { taskIndex++ }, modifier = Modifier.fillMaxWidth()) { Text("Complete challenges") }
                    }
                }
            }
        } else {
            Text("YOU DID IT!", color = Color(0xFF9DE3CC), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("All 4 wake-up challenges are complete. You can now dismiss the alarm.", color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(28.dp))
            Button(onClick = onAllTasksComplete, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Stop alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
