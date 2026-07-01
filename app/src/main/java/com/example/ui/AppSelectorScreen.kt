package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val allowedAppsString by viewModel.allowedApps.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    // Parse current allowed apps
    val allowedSet = remember(allowedAppsString) {
        if (allowedAppsString.isBlank()) mutableSetOf<String>()
        else allowedAppsString.split(",").toMutableSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == "es") "Aplicaciones Permitidas" else "Allowed Apps", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = if (lang == "es") 
                    "Elige hasta 5 aplicaciones importantes que podrás usar mientras estudias (ej. Duolingo, Kindle, Notas). Las apps que distraen (ej. Facebook) serán bloqueadas." 
                else 
                    "Choose up to 5 important apps you can use while studying (e.g. Duolingo, Kindle, Notes). Distracting apps (e.g. Facebook) will be blocked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${allowedSet.size} / 5",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (allowedSet.size == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (installedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(installedApps) { app ->
                        val isSelected = allowedSet.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (allowedSet.size < 5) {
                                            val newSet = allowedSet.toMutableSet().apply { add(app.packageName) }
                                            viewModel.setAllowedApps(newSet.joinToString(","))
                                        }
                                    } else {
                                        val newSet = allowedSet.toMutableSet().apply { remove(app.packageName) }
                                        viewModel.setAllowedApps(newSet.joinToString(","))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
