package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.FocusViewModel
import com.example.data.FocusSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalHours by viewModel.totalFocusedHours.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val allSessions: List<FocusSession> by viewModel.allSessions.collectAsState(initial = emptyList())
    val userGender by viewModel.gender.collectAsState()

    val failureProb = viewModel.getPredictiveFailureProbability()
    val failurePercent = (failureProb * 100).toInt()
    val isHighRisk = failurePercent > 50

    val analysisTitle = if (lang == "es") "Análisis Predictivo" else "Predictive Analysis"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(analysisTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analytics_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Real Stats Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$totalSessions",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == "es") "Sesiones" else "Sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", totalHours),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (lang == "es") "Horas Focus" else "Focus Hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$streak 🔥",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFF6D00)
                        )
                        Text(
                            text = if (lang == "es") "Racha" else "Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Prediction Alert (uses real Bayesian engine)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isHighRisk)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isHighRisk) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (lang == "es")
                                (if (isHighRisk) "⚠️ Riesgo de Fallo: $failurePercent%" else "✅ Bajo Riesgo: $failurePercent%")
                            else
                                (if (isHighRisk) "⚠️ Failure Risk: $failurePercent%" else "✅ Low Risk: $failurePercent%"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == "es")
                                "Motor Bayesiano Beta-Binomial: Probabilidad calculada con suavizado Laplaciano basada en tus sesiones reales a esta hora y día."
                            else
                                "Bayesian Beta-Binomial Engine: Probability calculated with Laplace smoothing based on your real sessions at this time and day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Chart: Real weekly productivity data
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (lang == "es") "Productividad Semanal vs Fracasos" else "Weekly Productivity vs Failures",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val errorColor = MaterialTheme.colorScheme.error
                    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                    // Build chart data from real DB stats
                    val chartData = (0..6).map { dow ->
                        val stat = weeklyStats.find { it.dayOfWeek == dow }
                        Pair(stat?.successCount?.toFloat() ?: 0f, stat?.failedCount?.toFloat() ?: 0f)
                    }
                    val maxVal = chartData.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1f) ?: 1f

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (weeklyStats.isEmpty()) {
                            // No data message
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang == "es") "Completa sesiones de enfoque para ver datos reales aquí 📊"
                                    else "Complete focus sessions to see real data here 📊",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 20.dp.toPx()
                                val spacing = (size.width - (barWidth * 2 * 7)) / 8
                                val maxBarHeight = size.height - 40.dp.toPx()

                                // Draw horizontal dashed lines
                                val lines = 4
                                for (i in 0..lines) {
                                    val y = i * (maxBarHeight / lines)
                                    drawLine(
                                        color = onSurfaceVariant.copy(alpha = 0.3f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }

                                for (i in 0 until 7) {
                                    val xOff = spacing + i * (barWidth * 2 + spacing)
                                    val (success, fail) = chartData[i]

                                    // Success bar
                                    val prodHeight = (success / maxVal) * maxBarHeight
                                    if (prodHeight > 0f) {
                                        drawRect(
                                            color = primaryColor,
                                            topLeft = Offset(xOff, maxBarHeight - prodHeight),
                                            size = Size(barWidth, prodHeight)
                                        )
                                    }

                                    // Failure bar
                                    val failHeight = (fail / maxVal) * maxBarHeight
                                    if (failHeight > 0f) {
                                        drawRect(
                                            color = errorColor,
                                            topLeft = Offset(xOff + barWidth, maxBarHeight - failHeight),
                                            size = Size(barWidth, failHeight)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (lang == "es") "Éxito" else "Success", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.width(24.dp))

                Box(modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.error))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (lang == "es") "Quiebre" else "Breaks", style = MaterialTheme.typography.bodyMedium)
            }

            // 3. GitHub Consistency Heatmap
            val baseColor = when (userGender) {
                "female" -> Color(0xFFFF62A9)
                "male" -> Color(0xFF2D69FF)
                else -> Color(0xFF00E676)
            }

            val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
            val sessionsByDate = remember(allSessions) {
                allSessions
                    .filter { it.isSuccess }
                    .groupBy { df.format(Date(it.startTime)) }
                    .mapValues { entry -> entry.value.sumOf { it.durationSeconds } / 60 }
            }

            val weekData: List<List<Int>> = remember(allSessions) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.WEEK_OF_YEAR, -11)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                List(12) {
                    List(7) {
                        val dateStr = df.format(calendar.time)
                        val minutes = sessionsByDate[dateStr] ?: 0
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        minutes
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (lang == "es") "Mapa de Calor de Consistencia" else "Focus Consistency Heatmap",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = baseColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            val daysLabels = if (lang == "es") listOf("Dom", "Mar", "Jue", "Sáb") else listOf("Sun", "Tue", "Thu", "Sat")
                            daysLabels.forEach { label ->
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            for (week in weekData) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (minutes in week) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    when {
                                                        minutes == 0 -> Color.White.copy(alpha = 0.08f)
                                                        minutes <= 10 -> baseColor.copy(alpha = 0.25f)
                                                        minutes <= 25 -> baseColor.copy(alpha = 0.5f)
                                                        minutes <= 60 -> baseColor.copy(alpha = 0.75f)
                                                        else -> baseColor
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Insight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Insight",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (totalSessions > 0) {
                            if (lang == "es")
                                "Has completado $totalSessions sesiones reales con ${String.format(java.util.Locale.US, "%.1f", totalHours)} horas de enfoque. El motor predictivo mejora con cada sesión que completas."
                            else
                                "You have completed $totalSessions real sessions with ${String.format(java.util.Locale.US, "%.1f", totalHours)} focus hours. The predictive engine improves with every session you complete."
                        } else {
                            if (lang == "es")
                                "Aún no tienes sesiones completadas. Empieza una para que el Motor Bayesiano comience a aprender tus patrones."
                            else
                                "No completed sessions yet. Start one so the Bayesian Engine can begin learning your patterns."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
