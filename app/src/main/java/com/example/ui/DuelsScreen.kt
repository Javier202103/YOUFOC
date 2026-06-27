package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelsScreen(viewModel: FocusViewModel) {
    val lang by viewModel.language.collectAsState()
    val activeDuels by viewModel.activeDuels.collectAsState()
    val squads by viewModel.punishmentSquads.collectAsState()
    val currentXp by viewModel.xp.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = if (lang == "es") {
        listOf("Duelos 1v1 ⚔️", "Squads de Castigo 🛡️")
    } else {
        listOf("1v1 Duels ⚔️", "Punishment Squads 🛡️")
    }

    val themeColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    // State for creating a duel
    var showCreateDuelDialog by remember { mutableStateOf(false) }
    var rivalNameInput by remember { mutableStateOf("") }
    var duelDurationInput by remember { mutableStateOf(4) }
    var duelWagerInput by remember { mutableStateOf(100) }

    // State for creating a squad
    var showCreateSquadDialog by remember { mutableStateOf(false) }
    var squadNameInput by remember { mutableStateOf("") }
    var squadPenaltyInput by remember { mutableStateOf(200) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("duels_screen")
    ) {
        // App header containing title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (lang == "es") "MULTIJUGADOR EXTREMO" else "EXTREME MULTIPLAYER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (lang == "es") "Presión social como catalizador" else "Social peer pressure as catalyst",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$currentXp XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = themeColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTabIndex = index 
                    },
                    modifier = Modifier.padding(vertical = 12.dp).testTag("duel_tab_$index")
                ) {
                    Text(
                        text = title,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedTabIndex == 0) {
                // DUELS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Create Challenge Button
                    Button(
                        onClick = { showCreateDuelDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("create_duel_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == "es") "Retar a un Rival ⚔️" else "Challenge a Rival ⚔️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Explanation Header Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Rules", tint = Color(0xFFFFB300))
                            Text(
                                text = if (lang == "es") 
                                    "¿Rendirte? Perderás tu XP apostada y se transferirá al rival al instante. ¡Enfócate con alma y vida!" 
                                    else "Giving up? You'll forfeit your wagered XP to the rival instantly. Attack your goals now!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Duels list
                    activeDuels.forEach { duel ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (duel.status == "Active") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Rival heading
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(duel.rivalAvatar, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "vs ${duel.rivalName}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "${duel.durationHours} ${if (lang == "es") "horas" else "hours"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // Status or wager tag
                                    when (duel.status) {
                                        "Won" -> {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == "es") "¡GANADO! +${duel.xpWager} XP" else "WON! +${duel.xpWager} XP",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        "Lost" -> {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == "es") "PERDIDO -${duel.xpWager} XP" else "LOST -${duel.xpWager} XP",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        else -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFB300).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Wager: ${duel.xpWager} XP",
                                                    color = Color(0xFFFF8F00),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Progress bars
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Player Progress indicator
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (lang == "es") "Tu Progreso" else "Your Progress",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${(duel.playerProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColor
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { duel.playerProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = themeColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    // Rival Progress indicator
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${duel.rivalName} Progress",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${(duel.rivalProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { duel.rivalProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }

                                // Interactive action buttons only if Active
                                if (duel.status == "Active") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Yield button
                                        OutlinedButton(
                                            onClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.yieldDuel(duel.id)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("yield_duel_${duel.id}")
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Yield")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (lang == "es") "Rendirse" else "Yield")
                                        }

                                        // Simulate Focus Complete
                                        Button(
                                            onClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.winDuel(duel.id)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("win_duel_${duel.id}")
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "Complete")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (lang == "es") "Concluir" else "Complete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // SQUADS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Create Squad button
                    Button(
                        onClick = { showCreateSquadDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("create_squad_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == "es") "Fundar Squad de Castigo 🛡️" else "Create Punishment Squad 🛡️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Warning Board about social distress & punishment squads
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CrisisAlert, contentDescription = "High Threat", tint = MaterialTheme.colorScheme.error)
                            Column {
                                Text(
                                    text = if (lang == "es") "PACTO DE HONOR COLECTIVO" else "COLLECTIVE OATH OF HONOR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (lang == "es") 
                                        "Si tú abres una app bloqueada o rompes el enfoque, ¡TODOS en tu escuadrón sufrirán una penalización drástica de XP y racha! No seas el eslabón débil." 
                                        else "If you open any banned app or shatter focus, the ENTIRE group suffers the XP & streak penalty! Protect each other.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Squad List
                    squads.forEach { squad ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = squad.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${squad.membersCount} ${if (lang == "es") "miembros" else "members"} • ${squad.cumulativeFocusHours}h ${if (lang == "es") "acumuladas" else "accumulated"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Status Badge or Fail label
                                    if (squad.status == "Failed") {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (lang == "es") "💥 DERROTADO" else "💥 SQUAD FAILED",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = if (lang == "es") "Castigo: -${squad.penaltyXp} XP" else "Penalty: -${squad.penaltyXp} XP",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Health Meter representing active Squad stamina before break
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (squad.health > 40) Icons.Default.Favorite else Icons.Default.HeartBroken,
                                            contentDescription = "Squad health",
                                            tint = if (squad.health > 40) Color(0xFFFF2D55) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lang == "es") "Stamina del Escuadrón" else "Squad Stamina",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${squad.health}%",
                                        fontWeight = FontWeight.Black,
                                        color = if (squad.health > 40) Color(0xFFFF2D55) else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { squad.health / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (squad.health > 40) Color(0xFFFF2D55) else MaterialTheme.colorScheme.error,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (squad.status == "Active") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    // Simulation control to show peer distraction
                                    OutlinedButton(
                                        onClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.triggerSquadFail(squad.id)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("trigger_squad_fail_${squad.id}")
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = "Simulate fail")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == "es") "Simular Error de Compañero" else "Simulate Teammate Slip-up")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE DUEL DIALOG
    if (showCreateDuelDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDuelDialog = false },
            title = {
                Text(
                    text = if (lang == "es") "Retar a un Duelista ⚔️" else "Challenge a Duelist ⚔️",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = rivalNameInput,
                        onValueChange = { rivalNameInput = it },
                        label = { Text(if (lang == "es") "Nombre del Rival" else "Rival Username") },
                        singleLine = true,
                        placeholder = { Text("E.g., FocusKing") },
                        modifier = Modifier.fillMaxWidth().testTag("rival_name_input")
                    )

                    Text(
                        text = if (lang == "es") "Duración del Desafío" else "Challenge Duration",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 8).forEach { hrs ->
                            val isSelected = duelDurationInput == hrs
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { duelDurationInput = hrs },
                                label = { Text("$hrs Hrs") }
                            )
                        }
                    }

                    Text(
                        text = if (lang == "es") "Apuesta de Experiencia (XP)" else "Experience Wager (XP)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(50, 100, 200).forEach { xp ->
                            val isSelected = duelWagerInput == xp
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { duelWagerInput = xp },
                                label = { Text("$xp XP") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rivalNameInput.isNotBlank()) {
                            viewModel.startDuel(rivalNameInput, duelDurationInput, duelWagerInput)
                            rivalNameInput = ""
                            showCreateDuelDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_duel")
                ) {
                    Text(if (lang == "es") "Declarar Duelo" else "Declare Duel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDuelDialog = false }) {
                    Text(if (lang == "es") "Cancelar" else "Cancel")
                }
            }
        )
    }

    // CREATE SQUAD DIALOG
    if (showCreateSquadDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSquadDialog = false },
            title = {
                Text(
                    text = if (lang == "es") "Fundar Squad de Castigo 🛡️" else "Form Punishment Squad 🛡️",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = squadNameInput,
                        onValueChange = { squadNameInput = it },
                        label = { Text(if (lang == "es") "Nombre del Escuadrón" else "Squad Name") },
                        singleLine = true,
                        placeholder = { Text("E.g., No Sins Clan") },
                        modifier = Modifier.fillMaxWidth().testTag("squad_name_input")
                    )

                    Text(
                        text = if (lang == "es") "Penalización Colectiva por Fallar" else "Group Penalty Upon Failure",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 200, 350).forEach { xp ->
                            val isSelected = squadPenaltyInput == xp
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { squadPenaltyInput = xp },
                                label = { Text("-$xp XP") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (squadNameInput.isNotBlank()) {
                            viewModel.createSquad(squadNameInput, squadPenaltyInput)
                            squadNameInput = ""
                            showCreateSquadDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("confirm_create_squad")
                ) {
                    Text(if (lang == "es") "Pactar Honor" else "Swear Honor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSquadDialog = false }) {
                    Text(if (lang == "es") "Cancelar" else "Cancel")
                }
            }
        )
    }
}
