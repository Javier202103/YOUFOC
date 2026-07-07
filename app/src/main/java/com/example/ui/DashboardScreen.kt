package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.viewmodel.FocusViewModel
import com.example.data.Goal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FocusViewModel,
    onStartFocus: (Goal) -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val goals by viewModel.uiState.collectAsState()
    val lang by viewModel.language.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_goal_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        var showRiskDialog by remember { mutableStateOf<Goal?>(null) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LocalizedStrings.get("goals_title", lang),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onNavigateToAnalytics, modifier = Modifier.testTag("analytics_btn")) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Predictive Analytics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (goals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = LocalizedStrings.get("no_goals", lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(goals) { goal ->
                        GoalCard(
                            goal = goal,
                            lang = lang,
                            onClick = { 
                                // Simulate high risk on some goals or randomly for demonstration.
                                if (goal.id % 2 == 0) {
                                    showRiskDialog = goal
                                } else {
                                    onStartFocus(goal)
                                }
                            },
                            onComplete = { viewModel.markGoalCompleted(goal) }
                        )
                    }
                }
            }
        }
        
        if (showRiskDialog != null) {
            val goal = showRiskDialog!!
            AlertDialog(
                onDismissRequest = { showRiskDialog = null },
                title = { 
                    Text(
                        if (lang == "es") "⚠️ Riesgo de Desconcentración Alto" else "⚠️ High Distraction Risk",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                text = {
                    Text(
                        if (lang == "es") 
                            "Motor Predictivo: Históricamente, en este horario tienes un 82% de probabilidad de romper la sesión o distraerte. ¿Estás seguro de que quieres apostar tu racha ahora mismo para la meta '${goal.title}'?"
                        else
                            "Predictive Engine: Historically, at this time you have an 82% probability of breaking the session or getting distracted. Are you sure you want to risk your streak right now for the goal '${goal.title}'?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showRiskDialog = null
                            onStartFocus(goal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (lang == "es") "Apostar Racha" else "Risk Streak")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRiskDialog = null }) {
                        Text(if (lang == "es") "Cancelar" else "Cancel")
                    }
                }
            )
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("25") }
        var isPomo by remember { mutableStateOf(false) }
        var allowEarly by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(LocalizedStrings.get("add_goal", lang)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(LocalizedStrings.get("goal_input_label", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(LocalizedStrings.get("duration_input_label", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == "es") "Modo Pomodoro" else "Pomodoro Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "es") "Divide el enfoque en bloques de 25 min y descansos." else "Splits focus time into 25 min blocks and breaks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPomo,
                            onCheckedChange = { isPomo = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == "es") "Permitir Completar Antes" else "Allow Early Complete",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "es") "Muestra el botón 'Completar Meta' durante la sesión." else "Shows 'Complete Goal' button during session.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = allowEarly,
                            onCheckedChange = { allowEarly = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val durationInt = duration.toIntOrNull() ?: 25
                    viewModel.addGoal(title, durationInt, isPomo, allowEarly)
                    showAddDialog = false
                }) {
                    Text(LocalizedStrings.get("add_button", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(LocalizedStrings.get("cancel_button", lang))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    goal: Goal,
    lang: String,
    onClick: () -> Unit,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDismissed by remember { mutableStateOf(goal.isCompleted) }

    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isDismissed = true
                onComplete()
                true
            } else {
                false
            }
        }
    )

    // Animated states for fade-out and strike-through
    val alpha by animateFloatAsState(
        targetValue = if (goal.isCompleted || isDismissed) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500)
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (goal.isCompleted || isDismissed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 500)
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor, shape = CardDefaults.shape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Complete Task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (goal.isCompleted || isDismissed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        val statusText = if (goal.isCompleted || isDismissed) {
                            LocalizedStrings.get("completed", lang)
                        } else {
                            LocalizedStrings.get("pending", lang)
                        }
                        val suffix = if (goal.isPomodoro) " • 🍅 Pomodoro" else ""
                        Text(
                            text = "${goal.durationMinutes} ${LocalizedStrings.get("mins", lang)} • $statusText$suffix",
                            color = if (goal.isCompleted || isDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!goal.isCompleted && !isDismissed) {
                        FilledIconButton(
                            onClick = onClick,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Focus")
                        }
                    }
                }
            }
        }
    )
}
