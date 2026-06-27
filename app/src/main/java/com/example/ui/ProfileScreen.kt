package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import com.example.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FocusViewModel,
    onNavigateToSettings: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val avatarIdx by viewModel.avatarIndex.collectAsState()
    val userGender by viewModel.gender.collectAsState()
    val interestsList by viewModel.interests.collectAsState()
    val longTermGoalsList by viewModel.longTermGoals.collectAsState()
    val customAvatarUri by viewModel.customAvatarUri.collectAsState()
    val nickname by viewModel.nickname.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.updateCustomAvatarUri(uri?.toString())
        }
    )
    
    // Gamification states
    val currentXp by viewModel.xp.collectAsState()
    val currentLevel by viewModel.level.collectAsState()
    val totalFocusedHours by viewModel.totalFocusedHours.collectAsState()
    val quoteStyleStrict by viewModel.quoteStyleStrict.collectAsState()

    // Key list of profile avatars
    val avatars = listOf("🚀", "🧠", "⚡", "🦉")
    
    // -------------------------------------------------------------
    // DYNAMIC COLOR PALETTE WORKER BASED ON THE SELECTED THEME CONTEXT
    // -------------------------------------------------------------
    val (primaryThemeColor, secondaryThemeColor, themeGradient, statsCardBg) = when (userGender) {
        "female" -> {
            // Feminine / Warm Soft Pink & Orchid Theme
            val prime = Color(0xFFFF62A9)
            val sec = Color(0xFFD08EFF)
            val grad = Brush.linearGradient(listOf(prime, sec))
            val cardBg = Color(0xFF2E202B)
            QuadColor(prime, sec, grad, cardBg)
        }
        "male" -> {
            // Masculine / Steel Custom Blue Theme
            val prime = Color(0xFF2D69FF)
            val sec = Color(0xFF5BA4FF)
            val grad = Brush.linearGradient(listOf(prime, sec))
            val cardBg = Color(0xFF16213D)
            QuadColor(prime, sec, grad, cardBg)
        }
        else -> {
            // Neutral / Cyber Cosmic Mint Theme
            val prime = Color(0xFF00E676)
            val sec = Color(0xFF00B0FF)
            val grad = Brush.linearGradient(listOf(prime, sec))
            val cardBg = Color(0xFF142426)
            QuadColor(prime, sec, grad, cardBg)
        }
    }

    // Dynamic quote calculation state
    var currentQuote by remember { mutableStateOf("") }
    
    // Regenerate appropriate quote when attributes change
    LaunchedEffect(userGender, lang, quoteStyleStrict) {
        currentQuote = viewModel.getRandomMotivationalQuote(lang, userGender, quoteStyleStrict)
    }

    // Inputs
    var newInterestInput by remember { mutableStateOf("") }
    var newGoalInput by remember { mutableStateOf("") }

    // -----------------------------------------------------------------
    // NESTED THEME PROVIDER: All M3 components automatically inherit colors
    // -----------------------------------------------------------------
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = primaryThemeColor,
            secondary = secondaryThemeColor,
            primaryContainer = primaryThemeColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryThemeColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("profile_screen_container"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // TITLEHEADER
            Text(
                text = if (lang == "es") "TU PERFIL DE ENFOQUE" else "YOUR FOCUS PROFILE",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primaryThemeColor,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // -------------------------------------------------------------
            // 1. DYNAMIC CONCENTRIC CIRCULAR PROGRESS BAR DISPLAY
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Calculation of modular level progress
                val xpProgressOffset = currentXp % 100
                val targetProgress = xpProgressOffset.toFloat() / 100f
                val progressFraction by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 1500),
                    label = "progressAnimation"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    
                    // Outer background ring track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )

                    // Forward sweep arc indicating current levels completion fraction
                    drawArc(
                        brush = themeGradient,
                        startAngle = -90f,
                        sweepAngle = progressFraction * 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner avatar display container (User Photo Selector)
                Box(
                    modifier = Modifier
                        .size(122.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, primaryThemeColor.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (customAvatarUri != null) {
                        AsyncImage(
                            model = customAvatarUri,
                            contentDescription = "User avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = avatars.getOrElse(avatarIdx) { "🧠" },
                            fontSize = 58.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Tap overlay indicator
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change Picture",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp).size(18.dp)
                        )
                    }
                    
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF00FF66), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                    )
                }

                // Focus Level overlap badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = primaryThemeColor,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "LVL $currentLevel",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = nickname,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            val xpLeft = 100 - (currentXp % 100)
            Text(
                text = "$currentXp XP (Lvl $currentLevel) • ${if (lang == "es") "Faltan" else "Need"} $xpLeft XP",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. AVATAR SELECTORS STRIP
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                avatars.forEachIndexed { index, symbol ->
                    val isSelected = avatarIdx == index
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (isSelected) primaryThemeColor else Color.White.copy(alpha = 0.05f),
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else primaryThemeColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setAvatar(index) }
                            .testTag("avatar_btn_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(symbol, fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. GAMIFIED METRICS PANEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1: Time focused
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = statsCardBg.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = primaryThemeColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f hrs", totalFocusedHours),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryThemeColor
                        )
                        Text(
                            text = if (lang == "es") "Tiempo Enfocado" else "Focused Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Column 2: Total points accumulated
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = statsCardBg.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = secondaryThemeColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currentXp XP",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = secondaryThemeColor
                        )
                        Text(
                            text = if (lang == "es") "Experiencia Total" else "Cumulative XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. PERSONALIZED GENDER & STYLE COLOR THEME SWAPCARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${LocalizedStrings.get("gender_label", lang)} / ${if (lang == "es") "Tema de Color" else "Color Theme"}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = primaryThemeColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == "es") "Cambiar tu género ajusta los tonos visuales del ecosistema." else "Changing gender adjusts the visual primary accents of You.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val genders = listOf("male" to "👨", "female" to "👩", "neutral" to "🧑")
                        genders.forEach { (genderKey, icon) ->
                            val isSelected = userGender == genderKey
                            val label = when (genderKey) {
                                "male" -> LocalizedStrings.get("male", lang)
                                "female" -> LocalizedStrings.get("female", lang)
                                else -> LocalizedStrings.get("neutral", lang)
                            }
                            
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGender(genderKey) },
                                label = { Text("$icon $label") },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = primaryThemeColor,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gender_chip_$genderKey")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. CUSTOMIZABLE MOTIVATIONAL QUOTES & PREFERENCE BOX
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = primaryThemeColor.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = primaryThemeColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LocalizedStrings.get("motivational_title", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryThemeColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.speakQuote(currentQuote, lang) }
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Listen Quote",
                                    tint = primaryThemeColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    currentQuote = viewModel.getRandomMotivationalQuote(lang, userGender, quoteStyleStrict)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh Quote",
                                    tint = primaryThemeColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = primaryThemeColor.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "\"$currentQuote\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // MOTIVATIONAL PREFERENCE SWITCH (STRICT COCHING TOGGLE)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == "es") "Estilo Espartano Estoico" else "Stoic Spartan Coaching",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryThemeColor
                            )
                            Text(
                                text = if (lang == "es") "Frases duras, exigentes y disciplinadas." else "Direct, firm, military-style discipline directives.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quoteStyleStrict,
                            onCheckedChange = { viewModel.toggleQuoteStyle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryThemeColor,
                                checkedTrackColor = primaryThemeColor.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("quote_preference_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. PREFERENCES / INTERESTS SELECTION BOX
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocalizedStrings.get("interests_label", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = primaryThemeColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        interestsList.chunked(2).forEach { rowList ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowList.forEach { interest ->
                                    SuggestionChip(
                                        onClick = { viewModel.toggleInterest(interest) },
                                        label = { Text(interest) },
                                        icon = {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = primaryThemeColor
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowList.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newInterestInput,
                            onValueChange = { newInterestInput = it },
                            placeholder = { Text(LocalizedStrings.get("add_interest_hint", lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_interest_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newInterestInput.isNotBlank()) {
                                    viewModel.addCustomInterest(newInterestInput)
                                    newInterestInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryThemeColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("add_interest_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add interest", tint = primaryThemeColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. LONG-TERM GOALS (OBJETIVOS A LARGO PLAZO) CARD LIST
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocalizedStrings.get("long_term_goals_label", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = primaryThemeColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    longTermGoalsList.forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = goal.isCompleted,
                                onCheckedChange = { viewModel.toggleLongTermGoal(goal.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryThemeColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.deleteLongTermGoal(goal.id) },
                                modifier = Modifier.testTag("delete_longgoal_${goal.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newGoalInput,
                            onValueChange = { newGoalInput = it },
                            placeholder = { Text(LocalizedStrings.get("add_goal_hint", lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_longgoal_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newGoalInput.isNotBlank()) {
                                    viewModel.addLongTermGoal(newGoalInput)
                                    newGoalInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryThemeColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("add_longgoal_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = primaryThemeColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. SYSTEM AND ACTION PREFERENCES BUTTONS
            OutlinedButton(
                onClick = onNavigateToSettings,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryThemeColor),
                border = BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_settings_btn")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(LocalizedStrings.get("app_settings", lang))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.toggleLanguage() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryThemeColor),
                border = BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_switch_btn")
            ) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(LocalizedStrings.get("switch_language", lang) + " (${if (lang == "es") "ES" else "EN"})")
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.testTag("logout_btn")
            ) {
                Text(
                    text = LocalizedStrings.get("logout", lang),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// HELPER DATA CLASS FOR THEMED COLOR COMBINATIONS
private data class QuadColor(
    val primary: Color,
    val secondary: Color,
    val brush: Brush,
    val cardBackground: Color
)
