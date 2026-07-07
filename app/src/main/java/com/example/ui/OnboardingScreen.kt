package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Bienvenido a FocusLock",
        description = "Esta no es una app de productividad normal. Aquí no hay opciones fáciles ni atajos. Estás a punto de comprometerte con tus metas de manera extrema.",
        icon = Icons.Default.RocketLaunch,
        color = Color(0xFF2D69FF) // Blue
    ),
    OnboardingPage(
        title = "El Muro Inquebrantable",
        description = "Si intentas abrir una app bloqueada como TikTok o Instagram, aparecerá un muro a pantalla completa. Te forzaremos a regresar a tu enfoque.",
        icon = Icons.Default.Warning,
        color = Color(0xFFFF9800) // Orange
    ),
    OnboardingPage(
        title = "Sin Ruta de Escape",
        description = "Una vez iniciado el temporizador, FocusLock no te dejará desinstalar la app ni forzar su detención. Estás atrapado con tu productividad por 15 días.",
        icon = Icons.Default.Lock,
        color = Color(0xFFE53935) // Red
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Pager Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                .size(if (isSelected) 10.dp else 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip or Empty Box
                    AnimatedVisibility(
                        visible = pagerState.currentPage < onboardingPages.size - 1,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TextButton(onClick = onFinish) {
                            Text("Saltar", color = Color.Gray)
                        }
                    }

                    if (pagerState.currentPage == onboardingPages.size - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Next / Finish Button
                    Button(
                        onClick = {
                            if (pagerState.currentPage < onboardingPages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinish()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onboardingPages[pagerState.currentPage].color
                        )
                    ) {
                        Text(
                            text = if (pagerState.currentPage == onboardingPages.size - 1) "Aceptar el Reto" else "Siguiente",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val pageData = onboardingPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = pageData.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = pageData.color
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pageData.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
