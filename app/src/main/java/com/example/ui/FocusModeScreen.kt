package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.viewmodel.FocusViewModel
import kotlinx.coroutines.delay

@Composable
fun FocusModeScreen(
    goalId: Int,
    viewModel: FocusViewModel,
    onComplete: () -> Unit
) {
    val goals by viewModel.uiState.collectAsState()
    val lang by viewModel.language.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val goal = goals.find { it.id == goalId } ?: return

    // Sound control
    val currentSound by viewModel.currentAmbientSound.collectAsState()

    // Stop ambient sound when leaving Focus Mode
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAmbientSound()
        }
    }

    // Pomodoro logic states
    var isBreak by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(goal.durationMinutes * 60) }
    var pomodoroFocusRemaining by remember { mutableStateOf(if (goal.isPomodoro) minOf(25 * 60, goal.durationMinutes * 60) else goal.durationMinutes * 60) }
    var breakRemaining by remember { mutableStateOf(5 * 60) }

    var currentQuote by remember { mutableStateOf(viewModel.getRandomMotivationalQuote(lang, gender)) }

    // Intercept back button to prevent leaving focus mode
    BackHandler(enabled = !goal.isCompleted) {
        // Do nothing, blocking exit
    }

    LaunchedEffect(isBreak, timeRemaining, breakRemaining) {
        if (!goal.isCompleted) {
            if (isBreak) {
                if (breakRemaining > 0) {
                    delay(1000L)
                    breakRemaining--
                } else {
                    // Break finished -> return to focus
                    isBreak = false
                    pomodoroFocusRemaining = minOf(25 * 60, timeRemaining)
                    viewModel.speakQuote(if (lang == "es") "Hora de enfocarse de nuevo." else "Time to focus again.", lang)
                }
            } else {
                if (timeRemaining > 0) {
                    delay(1000L)
                    timeRemaining--
                    if (goal.isPomodoro) {
                        pomodoroFocusRemaining--
                        if (pomodoroFocusRemaining <= 0 && timeRemaining > 0) {
                            // Focus period finished -> start break
                            isBreak = true
                            breakRemaining = 5 * 60
                            viewModel.speakQuote(if (lang == "es") "Buen trabajo. Tómate un respiro." else "Good job. Take a break.", lang)
                        }
                    }
                } else {
                    // All time finished
                    viewModel.markGoalCompleted(goal)
                    onComplete()
                }
            }
        }
    }

    // Dynamic background color based on Focus vs Break state
    val backgroundColor by animateColorAsState(
        targetValue = if (isBreak) Color(0xFF1B3D2F) else MaterialTheme.colorScheme.background,
        animationSpec = tween(1000)
    )

    val primaryThemeColor = when (gender) {
        "female" -> Color(0xFFFF62A9)
        "male" -> Color(0xFF2D69FF)
        else -> Color(0xFF00E676)
    }

    val displayColor = if (isBreak) Color(0xFF00FF88) else primaryThemeColor

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isBreak) {
                    if (lang == "es") "TIEMPO DE DESCANSO" else "BREAK TIME"
                } else {
                    LocalizedStrings.get("focus_mode", lang)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isBreak) Color(0xFF00FF88) else MaterialTheme.colorScheme.error,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Main Display (Timer or Breathing circle)
            if (isBreak) {
                // Breathing circle overlay
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(breathingScale)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                        .border(3.dp, Color(0xFF00FF88), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (breathingScale > 1f) {
                                if (lang == "es") "Inhala" else "Inhale"
                            } else {
                                if (lang == "es") "Exhala" else "Exhale"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val breakMin = breakRemaining / 60
                        val breakSec = breakRemaining % 60
                        Text(
                            text = String.format("%02d:%02d", breakMin, breakSec),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Focus Mode countdown timer
                val currentMin = timeRemaining / 60
                val currentSec = timeRemaining % 60
                val timeString = String.format("%02d:%02d", currentMin, currentSec)

                Text(
                    text = timeString,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = displayColor,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                )

                if (goal.isPomodoro) {
                    val pomodoroMin = pomodoroFocusRemaining / 60
                    val pomodoroSec = pomodoroFocusRemaining % 60
                    Text(
                        text = "Ciclo: ${String.format("%02d:%02d", pomodoroMin, pomodoroSec)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sound selection panel (Ambient Focus Music)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == "es") "🎧 Sonidos de Concentración" else "🎧 Focus Ambient Sounds",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = displayColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val sounds = listOf(
                        "none" to (if (lang == "es") "Silencio" else "Silent"),
                        "white_noise" to (if (lang == "es") "Ruido Blanco" else "White Noise"),
                        "ocean" to (if (lang == "es") "Olas" else "Ocean"),
                        "binaural" to (if (lang == "es") "Binaural" else "Binaural"),
                        "space" to (if (lang == "es") "Espacio" else "Space")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sounds.forEach { (sType, sName) ->
                            val isSel = currentSound == sType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) displayColor else Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        if (isSel) viewModel.stopAmbientSound() else viewModel.playAmbientSound(sType)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sName,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip break button / info
            if (isBreak) {
                Button(
                    onClick = {
                        isBreak = false
                        pomodoroFocusRemaining = minOf(25 * 60, timeRemaining)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip break")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == "es") "Saltar Descanso" else "Skip Break", fontWeight = FontWeight.Bold)
                }
            } else {
                // Focus quote
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = displayColor.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"$currentQuote\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.markGoalCompleted(goal)
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = displayColor, contentColor = if (isBreak) Color.Black else Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Complete")
                Spacer(modifier = Modifier.width(8.dp))
                Text(LocalizedStrings.get("complete_unlock", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}
