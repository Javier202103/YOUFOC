package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.viewmodel.FocusViewModel

@Composable
fun RankingScreen(viewModel: FocusViewModel) {
    val lang by viewModel.language.collectAsState()
    val totalHours by viewModel.totalFocusedHours.collectAsState()
    val currentXp by viewModel.xp.collectAsState()
    val currentLevel by viewModel.level.collectAsState()
    val userNickname by viewModel.nickname.collectAsState()
    val customAvatarUri by viewModel.customAvatarUri.collectAsState()
    val avatarIdx by viewModel.avatarIndex.collectAsState()

    val avatars = listOf("🚀", "🧠", "⚡", "🦉")
    val userHoursStr = String.format(java.util.Locale.US, "%.0f", totalHours)
    val hrsLabel = LocalizedStrings.get("hrs", lang)
    val youLabel = if (userNickname.isNotBlank()) "$userNickname (${LocalizedStrings.get("you", lang)})" else LocalizedStrings.get("you", lang)

    // Fetch online ranking on screen load
    val onlineRanking by viewModel.onlineRanking.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncProfileToCloud()
        viewModel.fetchOnlineRanking()
    }

    // Build entry list: online users OR local NPCs as fallback
    val userEmoji = avatars.getOrElse(avatarIdx) { "⭐" }
    val userEntry = RankEntry(youLabel, totalHours, userEmoji, isUser = true, customAvatarUri = customAvatarUri)

    val allEntries = if (onlineRanking.isNotEmpty()) {
        // Use real online data + ensure user is included
        val onlineEntries = onlineRanking
            .filter { it.nickname != userNickname }
            .map { RankEntry(it.nickname, it.totalHours, avatars.getOrElse(it.avatarIndex) { "🐼" }) }
        (onlineEntries + userEntry).sortedByDescending { it.hours }
    } else {
        // Offline fallback with NPCs
        val fixedRivals = listOf(
            RankEntry("Alex M.", 120f, "🐙"),
            RankEntry("Maria S.", 95f, "🦊"),
            RankEntry("Juan D.", 84f, "🐉"),
            RankEntry("Carlos V.", 42f, "🐯"),
            RankEntry("Elena P.", 36f, "🦁"),
            RankEntry("Sofia R.", 10f, "🐼")
        )
        (fixedRivals + userEntry).sortedByDescending { it.hours }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ranking_screen")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = LocalizedStrings.get("ranking_title", lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (lang == "es") "Tabla de líderes" else "Leaderboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Your Stats badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL $currentLevel", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(if (lang == "es") "Nivel" else "Level", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$currentXp", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("XP", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$userHoursStr $hrsLabel", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(if (lang == "es") "Enfoque" else "Focus", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PODIUM (Top 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            if (allEntries.size >= 3) {
                // Silver (Rank 2)
                PodiumItem(
                    rank = 2,
                    name = allEntries[1].name,
                    score = String.format(java.util.Locale.US, "%.0f %s", allEntries[1].hours, hrsLabel),
                    height = 140.dp,
                    color = if (allEntries[1].isUser) MaterialTheme.colorScheme.primary else Color(0xFFC0C0C0),
                    medalColor = Color(0xFFE8E8E8),
                    emoji = allEntries[1].emoji,
                    customAvatarUri = allEntries[1].customAvatarUri
                )

                // Gold (Rank 1)
                PodiumItem(
                    rank = 1,
                    name = allEntries[0].name,
                    score = String.format(java.util.Locale.US, "%.0f %s", allEntries[0].hours, hrsLabel),
                    height = 180.dp,
                    color = if (allEntries[0].isUser) MaterialTheme.colorScheme.primary else Color(0xFFFFD700),
                    medalColor = Color(0xFFFFF7B0),
                    emoji = allEntries[0].emoji,
                    customAvatarUri = allEntries[0].customAvatarUri
                )

                // Bronze (Rank 3)
                PodiumItem(
                    rank = 3,
                    name = allEntries[2].name,
                    score = String.format(java.util.Locale.US, "%.0f %s", allEntries[2].hours, hrsLabel),
                    height = 110.dp,
                    color = if (allEntries[2].isUser) MaterialTheme.colorScheme.primary else Color(0xFFCD7F32),
                    medalColor = Color(0xFFF2A45C),
                    emoji = allEntries[2].emoji,
                    customAvatarUri = allEntries[2].customAvatarUri
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // REST OF THE LIST
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(allEntries.drop(3)) { index, entry ->
                val actualRank = index + 4
                val isCurrentUser = entry.isUser

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$actualRank",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (entry.customAvatarUri != null) {
                                        AsyncImage(
                                            model = entry.customAvatarUri,
                                            contentDescription = "User Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = entry.emoji,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = entry.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = String.format(java.util.Locale.US, "%.0f %s", entry.hours, hrsLabel),
                            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumItem(
    rank: Int,
    name: String,
    score: String,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    medalColor: Color,
    emoji: String = "🐼",
    customAvatarUri: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        // Avatar circle above the podium
        Surface(
            shape = CircleShape,
            color = medalColor.copy(alpha = 0.3f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (customAvatarUri != null) {
                    AsyncImage(
                        model = customAvatarUri,
                        contentDescription = "Podium Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = score,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(height)
                .width(60.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = medalColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = rank.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private data class RankEntry(
    val name: String,
    val hours: Float,
    val emoji: String = "🐼",
    val isUser: Boolean = false,
    val customAvatarUri: String? = null
)
