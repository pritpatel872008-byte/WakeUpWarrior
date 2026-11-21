package com.wakeupwarrior.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setContent {
            MaterialTheme {
                BackHandler(enabled = true) { }
                AlarmChallengeFlow(onAllTasksComplete = { stopAlarm() })
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
    val context = LocalContext.current
    var taskIndex by remember { mutableIntStateOf(0) }
    var quizAnswered by remember { mutableStateOf(false) }
    var quizMessage by remember { mutableStateOf("Choose the correct answer.") }

    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraDenied by remember { mutableStateOf(false) }
    var danceStarted by remember { mutableStateOf(false) }
    var danceSeconds by remember { mutableIntStateOf(20) }
    var motionDetected by remember { mutableStateOf(false) }
    var movementSeconds by remember { mutableIntStateOf(0) }
    var dancePassed by remember { mutableStateOf(false) }

    var brushStarted by remember { mutableStateOf(false) }
    var brushSeconds by remember { mutableIntStateOf(120) }

    var wheelSpinning by remember { mutableStateOf(false) }
    var wheelResult by remember { mutableStateOf<String?>(null) }
    val wheelRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraDenied = !granted
        if (granted) {
            danceStarted = true
        }
    }

    LaunchedEffect(danceStarted, danceSeconds) {
        if (danceStarted && danceSeconds > 0) {
            delay(1000)
            danceSeconds--
            if (motionDetected) movementSeconds++
            if (danceSeconds == 1 && movementSeconds >= 8) dancePassed = true
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
        "Allow camera access and keep moving. At least 8 seconds of movement must be detected during the 20-second challenge.",
        "Brush for the full two-minute timer before continuing.",
        "Spin the wheel. Wait for it to finish to reveal your result."
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
            Spacer(Modifier.height(24.dp))

            when (taskIndex) {
                0 -> {
                    Text("What is 7 × 8?", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp))
                    listOf("54", "56", "64", "48").forEach { answer ->
                        OutlinedButton(
                            onClick = {
                                if (!quizAnswered) {
                                    if (answer == "56") {
                                        quizAnswered = true
                                        quizMessage = "Correct! Moving to the next challenge..."
                                    } else quizMessage = "Incorrect. Try again!"
                                }
                            },
                            enabled = !quizAnswered,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (quizAnswered && answer == "56") "56  ✓ Correct" else answer, color = Color.White) }
                    }
                    Text(quizMessage, color = if (quizAnswered) Color(0xFF9DE3CC) else Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    LaunchedEffect(quizAnswered) {
                        if (quizAnswered) {
                            delay(700)
                            taskIndex = 1
                        }
                    }
                }
                1 -> {
                    Text("$danceSeconds s", color = Color(0xFF9DE3CC), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text("Movement detected: $movementSeconds / 8 seconds", color = Color.White, fontSize = 14.sp)
                    if (!danceStarted) {
                        Button(onClick = {
                            if (cameraGranted) danceStarted = true
                            else permissionLauncher.launch(Manifest.permission.CAMERA)
                        }, modifier = Modifier.fillMaxWidth()) { Text("Allow camera & start dance") }
                        if (cameraDenied) {
                            Text("Camera permission is needed to verify movement.", color = Color.White, fontSize = 13.sp)
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                            }, modifier = Modifier.fillMaxWidth()) { Text("Open app settings", color = Color.White) }
                        }
                    } else if (danceSeconds > 0) {
                        CameraMotionPreview(
                            enabled = cameraGranted,
                            onMotionChanged = { motionDetected = it }
                        )
                        Text(if (motionDetected) "Movement detected ✓ Keep dancing!" else "Move in front of the camera!", color = if (motionDetected) Color(0xFF9DE3CC) else Color.White, fontSize = 14.sp)
                    } else if (dancePassed) {
                        Text("Dance challenge passed!", color = Color(0xFF9DE3CC), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { taskIndex = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    } else {
                        Text("Not enough movement detected. Try again!", color = Color.White, fontSize = 15.sp)
                        Button(onClick = {
                            danceSeconds = 20
                            movementSeconds = 0
                            dancePassed = false
                            danceStarted = true
                        }, modifier = Modifier.fillMaxWidth()) { Text("Retry dance challenge") }
                    }
                }
                2 -> {
                    Text(if (brushSeconds > 0) String.format("%02d:%02d", brushSeconds / 60, brushSeconds % 60) else "Brushing time complete!", color = Color(0xFF9DE3CC), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    if (!brushStarted) {
                        Button(onClick = { brushStarted = true }, modifier = Modifier.fillMaxWidth()) { Text("Start 2-minute brush timer") }
                    } else if (brushSeconds == 0) {
                        Button(onClick = { taskIndex = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    } else Text("Keep brushing until the timer finishes.", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                }
                3 -> {
                    Text(
                        if (wheelSpinning) "SPINNING..." else wheelResult ?: "Ready to spin?",
                        color = Color(0xFF9DE3CC), fontSize = 23.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("🎯", modifier = Modifier.graphicsLayer { rotationZ = wheelRotation.value }.padding(12.dp), fontSize = 88.sp)
                    if (wheelResult == null && !wheelSpinning) {
                        Button(onClick = {
                            wheelSpinning = true
                            scope.launch {
                                val prize = listOf("Today is your day!", "You are unstoppable!", "Great things await!", "Make it count!", "You are a winner!").random()
                                val extraSpin = Random.nextInt(0, 4) * 360f
                                wheelRotation.animateTo(
                                    targetValue = wheelRotation.value + 360f * 5 + extraSpin,
                                    animationSpec = tween(durationMillis = 3200)
                                )
                                wheelResult = prize
                                wheelSpinning = false
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("SPIN THE LUCKY WHEEL") }
                    } else if (wheelSpinning) {
                        Text("Wait until the wheel stops...", color = Color.White, fontSize = 14.sp)
                    } else {
                        Text(wheelResult ?: "", color = Color.White, fontSize = 17.sp)
                        Button(onClick = { taskIndex = 4 }, modifier = Modifier.fillMaxWidth()) { Text("Complete challenges") }
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

@Composable
private fun CameraMotionPreview(enabled: Boolean, onMotionChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            androidx.camera.view.PreviewView(ctx).apply {
                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }
                    val analysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    var previous: IntArray? = null
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { image ->
                        val plane = image.planes.firstOrNull()
                        if (plane != null) {
                            val buffer = plane.buffer
                            val width = image.width
                            val height = image.height
                            val samples = IntArray(100)
                            var index = 0
                            for (gy in 0 until 10) {
                                for (gx in 0 until 10) {
                                    val x = (gx * width / 10).coerceAtMost(width - 1)
                                    val y = (gy * height / 10).coerceAtMost(height - 1)
                                    val offset = y * plane.rowStride + x * plane.pixelStride
                                    samples[index++] = buffer.get(offset).toInt() and 0xFF
                                }
                            }
                            val old = previous
                            if (old != null) {
                                var changed = 0
                                for (i in samples.indices) if (kotlin.math.abs(samples[i] - old[i]) > 18) changed++
                                onMotionChanged(changed >= 12)
                            }
                            previous = samples
                        }
                        image.close()
                    }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                    } catch (_: Exception) {
                        onMotionChanged(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxWidth().height(190.dp)
    )
}
