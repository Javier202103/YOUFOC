package com.example.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AdminReceiver
import com.example.service.FocusAccessibilityService
import com.example.viewmodel.FocusViewModel

@Composable
fun PermissionsScreen(
    viewModel: FocusViewModel,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlay by remember { mutableStateOf(checkOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    var hasAdmin by remember { mutableStateOf(checkAdminPermission(context)) }

    // Re-check permissions when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = checkOverlayPermission(context)
                hasAccessibility = checkAccessibilityPermission(context)
                hasAdmin = checkAdminPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allGranted = hasOverlay && hasAccessibility && hasAdmin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Activación del Escudo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para que FocusLock sea implacable, debes otorgar estos 3 permisos obligatorios. Sin ellos, el bloqueo no funcionará.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Overlay Permission
        PermissionCard(
            title = "Superposición de Pantalla",
            description = "Permite dibujar el escudo irrompible sobre las apps bloqueadas.",
            icon = Icons.Default.Visibility,
            isGranted = hasOverlay,
            onClick = {
                if (!hasOverlay) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            }
        )

        // 2. Accessibility Permission
        PermissionCard(
            title = "Servicio de Accesibilidad",
            description = "El ojo que detecta al instante cuando abres una app prohibida.",
            icon = Icons.Default.Security,
            isGranted = hasAccessibility,
            onClick = {
                if (!hasAccessibility) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )

        // 3. Device Admin Permission
        PermissionCard(
            title = "Administrador de Dispositivo",
            description = "Blindaje extremo: Impide que borres la app en un momento de debilidad.",
            icon = Icons.Default.Lock,
            isGranted = hasAdmin,
            onClick = {
                if (!hasAdmin) {
                    val componentName = ComponentName(context, AdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Necesario para evitar desinstalaciones durante el enfoque.")
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (allGranted) {
                    onPermissionsGranted()
                }
            },
            enabled = allGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allGranted) Color(0xFF2D69FF) else Color.Gray
            )
        ) {
            Text(
                text = if (allGranted) "¡Todo Listo, Entrar!" else "Faltan Permisos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF00E676).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun checkAccessibilityPermission(context: Context): Boolean {
    var isAccessibilityEnabled = 0
    val service = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
    try {
        isAccessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        // Ignored
    }
    if (isAccessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            return settingValue.contains(service, ignoreCase = true)
        }
    }
    return false
}

fun checkAdminPermission(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val componentName = ComponentName(context, AdminReceiver::class.java)
    return dpm.isAdminActive(componentName)
}
