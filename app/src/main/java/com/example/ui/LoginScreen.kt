package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: FocusViewModel,
    onLoginSuccess: () -> Unit
) {
    val lang by viewModel.language.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val userNickname by viewModel.nickname.collectAsState()
    val customAvatarUri by viewModel.customAvatarUri.collectAsState()
    val avatarIdx by viewModel.avatarIndex.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    val avatars = listOf("🚀", "🧠", "⚡", "🦉")

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    if (!isRegistered) {
        // ONBOARDING / REGISTRATION FLOW
        var nicknameInput by remember { mutableStateOf("") }
        var pinInput by remember { mutableStateOf("") }
        var selectedGender by remember { mutableStateOf("neutral") }
        var selectedAvatarIndex by remember { mutableStateOf(0) }
        var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
        val selectedInterests = remember { mutableStateListOf("Programación 💻", "Deporte y Salud 🏃") }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri -> pickedImageUri = uri }
        )

        val (primaryColor, themeGrad) = when (selectedGender) {
            "female" -> Color(0xFFFF62A9) to Brush.linearGradient(listOf(Color(0xFFFF62A9), Color(0xFFD08EFF)))
            "male" -> Color(0xFF2D69FF) to Brush.linearGradient(listOf(Color(0xFF2D69FF), Color(0xFF5BA4FF)))
            else -> Color(0xFF00E676) to Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF)))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (lang == "es") "Crea tu Cuenta" else "Create Account", fontWeight = FontWeight.Bold) },
                    actions = {
                        TextButton(onClick = { viewModel.toggleLanguage() }) {
                            Text(
                                text = if (lang == "es") "EN 🇺🇸" else "ES 🇪🇸",
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (lang == "es") "¡Bienvenido a You!" else "Welcome to You!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (lang == "es") "Configura tu perfil de enfoque extremo." else "Set up your extreme focus profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, primaryColor, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (pickedImageUri != null) {
                        AsyncImage(
                            model = pickedImageUri,
                            contentDescription = "Custom Profile Pic",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = avatars.getOrElse(selectedAvatarIndex) { "🧠" },
                            fontSize = 50.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Edit photo",
                            tint = Color.White,
                            modifier = Modifier.padding(bottom = 6.dp).size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == "es") "Toca para elegir foto de la galería" else "Tap to choose gallery photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (pickedImageUri == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        avatars.forEachIndexed { idx, emo ->
                            val isSel = selectedAvatarIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isSel) primaryColor else Color.White.copy(alpha = 0.05f),
                                        CircleShape
                                    )
                                    .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                                    .clickable { selectedAvatarIndex = idx }
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Text(emo, fontSize = 20.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    TextButton(onClick = { pickedImageUri = null }) {
                        Text(if (lang == "es") "Usar emoji en su lugar" else "Use emoji instead", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text(if (lang == "es") "Apodo / Nombre" else "Nickname / Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text(if (lang == "es") "PIN de Seguridad (4 dígitos)" else "Security PIN (4 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "es") "Género e Identidad Visual" else "Gender & Visual Theme",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val genders = listOf("male" to "👨 Hombre", "female" to "👩 Mujer", "neutral" to "🧑 Neutro")
                            genders.forEach { (gKey, label) ->
                                val isSel = selectedGender == gKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) primaryColor else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSel) Color.White else primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { selectedGender = gKey }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "es") "Selecciona tus Gustos / Intereses" else "Select Focus Tastes / Interests",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val availableInterests = listOf("Programación 💻", "Diseño Gráfico 🎨", "Lectura 📚", "Deporte y Salud 🏃", "Filosofía 🏛️")
                        availableInterests.chunked(2).forEach { rowList ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                rowList.forEach { interest ->
                                    val isSel = selectedInterests.contains(interest)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            if (isSel) selectedInterests.remove(interest) else selectedInterests.add(interest)
                                        },
                                        label = { Text(interest) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowList.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (nicknameInput.isNotBlank() && pinInput.length >= 4) {
                            viewModel.registerUser(
                                nickname = nicknameInput,
                                pin = pinInput,
                                gender = selectedGender,
                                avatarIndex = selectedAvatarIndex,
                                customAvatarUri = pickedImageUri?.toString(),
                                interests = selectedInterests.toList()
                            )
                        }
                    },
                    enabled = nicknameInput.isNotBlank() && pinInput.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (lang == "es") "Comenzar Viaje de Enfoque" else "Begin Focus Journey",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    } else {
        // SECURE PIN LOCKSCREEN
        var pinVerify by remember { mutableStateOf("") }
        val (primaryColor, _) = when (viewModel.gender.collectAsState().value) {
            "female" -> Color(0xFFFF62A9) to Brush.linearGradient(listOf(Color(0xFFFF62A9), Color(0xFFD08EFF)))
            "male" -> Color(0xFF2D69FF) to Brush.linearGradient(listOf(Color(0xFF2D69FF), Color(0xFF5BA4FF)))
            else -> Color(0xFF00E676) to Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF)))
        }

        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (customAvatarUri != null) {
                        AsyncImage(
                            model = customAvatarUri,
                            contentDescription = "User Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = avatars.getOrElse(avatarIdx) { "🧠" },
                            fontSize = 50.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${if (lang == "es") "Hola, " else "Hello, "} $userNickname",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "es") "Ingresa tu PIN para desbloquear" else "Enter PIN to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = pinVerify,
                    onValueChange = {
                        if (it.length <= 4) {
                            pinVerify = it
                            if (it.length == 4) {
                                viewModel.verifyPinAndLogin(it)
                            }
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(200.dp)
                        .testTag("pin_verify_field"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )

                if (loginError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (lang == "es") "¡PIN Incorrecto!" else "Incorrect PIN!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
