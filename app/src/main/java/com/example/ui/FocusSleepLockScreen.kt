package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.FocusViewModel
import java.util.Calendar

@Composable
fun FocusSleepLockScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val isSimulation by viewModel.forceSleepSimulation.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Infinite animation for stars shimmering and pulse glow
    val infiniteTransition = rememberInfiniteTransition(label = "stars_glow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val moonPulse by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moon_pulse"
    )

    // State for temporary bypass confirmation dialog
    var showBypassDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712), // Deep Space Black
                        Color(0xFF0B1530), // Cosmic Violet/Navy
                        Color(0xFF141F45)  // Nebula Dusk Blue
                    )
                )
            )
            .padding(16.dp)
            .testTag("focus_sleep_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Dynamic Starry Moon Canvas Header
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw glowing outer circles for the moon
                    drawCircle(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.05f * alphaAnim),
                        radius = w / 2.8f + moonPulse
                    )
                    drawCircle(
                        color = Color(0xFF90CAF9).copy(alpha = 0.08f),
                        radius = w / 3.4f + (moonPulse / 2f)
                    )

                    // Draw stars randomly inside canvas bounding box
                    val starsList = listOf(
                        Offset(w * 0.15f, h * 0.2f),
                        Offset(w * 0.85f, h * 0.15f),
                        Offset(w * 0.25f, h * 0.8f),
                        Offset(w * 0.80f, h * 0.75f),
                        Offset(w * 0.10f, h * 0.5f),
                        Offset(w * 0.90f, h * 0.45f)
                    )
                    starsList.forEachIndexed { i, offset ->
                        val scaleFactor = if (i % 2 == 0) alphaAnim else (1f - alphaAnim)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f * scaleFactor),
                            radius = (4 + (i % 3) * 2).toFloat() * scaleFactor,
                            center = offset
                        )
                    }

                    // Draw Crescent Moon path elegantly
                    // Simple crescent drawn by subtracting or drawing custom circles
                    drawCircle(
                        color = Color(0xFFFFD54F), // Tender Yellow Moon
                        radius = w / 4.5f,
                        center = center
                    )
                    // Subtract overlay circle in dark space background color to make crescent
                    drawCircle(
                        color = Color(0xFF070E20), // Matches general gradient top-middle
                        radius = w / 4.5f,
                        center = center.copy(x = center.x - w / 12f, y = center.y - h / 30f)
                    )
                }

                // Mini Sleep Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 40.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep active",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sleep Block Label
            Text(
                text = if (lang == "es") "MODO RECARGA MENTAL ACTIVO 😴" else "MENTAL RECHARGE MODE ACTIVE 😴",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Elegant primary quote message block
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == "es") {
                            "\"No puedes completar tus objetivos con el cerebro cansado. El descanso de calidad forja la excelencia.\""
                        } else {
                            "\"You cannot complete goals with a tired brain. High-quality rest builds peak human excellence.\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (lang == "es") {
                            "Por salud y neurociencia, el bloqueo estricto está activo hasta las 6:00 AM."
                        } else {
                            "For neurological health & strict stamina, the mandatory lock remains active until 6:00 AM."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current state timer summary description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show current hour or simulated state
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time limits",
                        tint = Color(0xFF90CAF9)
                    )
                    Text(
                        text = if (lang == "es") {
                            "Regla de Horario: 11:00 PM - 06:00 AM"
                        } else {
                            "Schedule Rule: 11:00 PM - 06:00 AM"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Simulation Indicator Badge if turned on
            if (isSimulation) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = if (lang == "es") "SIMULACIÓN FORZADA ACTIVA" else "FORCED SIMULATION ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Gamified Emergency bypass triggers
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showBypassDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("emergency_bypass_btn")
            ) {
                Icon(Icons.Default.LocalHospital, contentDescription = "Emergency")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lang == "es") "Urgencia Médica (Omitir con Castigo) ⚠️" else "Medical Emergency (Bypass with Penalty) ⚠️",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Emergency Lock screen verification Dialog
        if (showBypassDialog) {
            AlertDialog(
                onDismissRequest = { showBypassDialog = false },
                title = {
                    Text(
                        text = if (lang == "es") "⚠️ ¿CONFIRMAR OMISIÓN EXTREMA?" else "⚠️ CONFIRM CRITICAL BYPASS?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = if (lang == "es") {
                            "Saltarse las horas de sueño obligatorio tiene un costo masivo para motivarte. Se cancelará la simulación o bloqueará temporalmente el protector de descanso, pero perderás -50 Puntos de Experiencia (XP) al instante."
                        } else {
                            "Skipping mandatory sleep rest limits represents a breakdown in discipline. Overriding this sleep shield will immediately penalize you by deduct -50 XP."
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addXp(-50)
                            if (isSimulation) {
                                viewModel.toggleForceSleepSimulation()
                            } else {
                                // Deactivate shield lock globally by disabling Focus Sleep mode
                                viewModel.toggleFocusSleepEnabled()
                            }
                            showBypassDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_sleep_bypass")
                    ) {
                        Text(
                            text = if (lang == "es") "Aceptar Castigo e Interrumpir" else "Accept Penalty & Break Lock"
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showBypassDialog = false }
                    ) {
                        Text(if (lang == "es") "Seguir Durmiendo 💤" else "Keep Sleeping 💤")
                    }
                }
            )
        }
    }
}
