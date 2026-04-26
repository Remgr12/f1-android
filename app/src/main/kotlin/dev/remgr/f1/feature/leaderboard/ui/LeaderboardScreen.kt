package dev.remgr.f1.feature.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding
import dev.remgr.f1.core.util.F1Images

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(
    onNavigateToSettings: () -> Unit,
    vm: LeaderboardViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        if (state is LeaderboardViewModel.UiState.Success) {
            val nextGp = (state as LeaderboardViewModel.UiState.Success).nextGp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp), // Reduced vertical padding from 12 to 6
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (nextGp != null) {
                        Text(
                            text = "Next: ${nextGp.name}",
                            style = MaterialTheme.typography.titleSmall, // Smaller style
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In ${nextGp.daysUntil} days",
                            style = MaterialTheme.typography.labelSmall, // Smaller style
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "F1 Standings",
                            style = MaterialTheme.typography.titleMedium, // Smaller style
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(32.dp) // Smaller button
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp) // Smaller icon
                    )
                }
            }
        }

        TabRow(selectedTabIndex = pagerState.currentPage) {
            listOf("Drivers", "Constructors").forEachIndexed { idx, label ->
                Tab(
                    selected = pagerState.currentPage == idx,
                    onClick  = { scope.launch { pagerState.animateScrollToPage(idx) } },
                    text     = { Text(label, modifier = Modifier.padding(vertical = 10.dp)) }, // Reduced vertical padding from 14 to 10
                )
            }
        }

        when (val s = state) {
            is LeaderboardViewModel.UiState.Loading -> LoadingBox()
            is LeaderboardViewModel.UiState.Error   -> ErrorBox(s.message, vm::retry)
            is LeaderboardViewModel.UiState.Success -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    if (page == 0) DriverList(s.drivers)
                    else ConstructorList(s.constructors)
                }
            }
        }
    }
}

@Composable
private fun DriverList(drivers: List<DriverStanding>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), // Reduced vertical padding from 8 to 4
        verticalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing from 8 to 4
    ) {
        items(drivers, key = { it.driverNumber }) { DriverCard(it) }
    }
}

@Composable
private fun DriverCard(s: DriverStanding) {
    val teamColor = rememberTeamColor(s.teamColour)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(8.dp), // Reduced padding from 12 to 8
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Reduced spacing from 12 to 8
        ) {
            // Position badge
            Box(
                modifier        = Modifier.size(32.dp).clip(CircleShape).background(teamColor.copy(alpha = 0.2f)), // Reduced size from 40 to 32
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "${s.position}",
                    style      = MaterialTheme.typography.bodyMedium, // Smaller text
                    fontWeight = FontWeight.Bold,
                    color      = teamColor,
                )
            }

            AsyncImage(
                model            = s.headshotUrl,
                contentDescription = s.fullName,
                modifier         = Modifier.size(40.dp).clip(CircleShape), // Reduced size from 48 to 40
                contentScale     = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = s.nameAcronym, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) // Smaller text
                Text(text = s.teamName, style = MaterialTheme.typography.labelSmall, color = teamColor) // Smaller text
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${s.points}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) // Smaller text
                Text(text = "PTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Team color stripe
            Box(modifier = Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
        }
    }
}

@Composable
private fun ConstructorList(constructors: List<ConstructorStanding>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), // Reduced vertical padding from 8 to 4
        verticalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing from 8 to 4
    ) {
        items(constructors, key = { it.teamName }) { ConstructorCard(it) }
    }
}

@Composable
private fun ConstructorCard(s: ConstructorStanding) {
    val teamColor = rememberTeamColor(s.teamColour)
    val logoUrl = F1Images.getConstructorLogoUrl(s.teamName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(10.dp), // Reduced padding from 16 to 10
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp), // Reduced spacing from 16 to 10
        ) {
            Text(
                text       = "${s.position}",
                style      = MaterialTheme.typography.titleMedium, // Smaller text
                fontWeight = FontWeight.Bold,
                color      = teamColor,
                modifier   = Modifier.width(28.dp),
                textAlign  = TextAlign.Center,
            )

            if (logoUrl != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp) // Reduced size from 56 to 44
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = s.teamName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                HorizontalDivider(
                    modifier  = Modifier.height(36.dp).width(3.dp).clip(RoundedCornerShape(2.dp)), // Reduced size
                    color     = teamColor,
                    thickness = 3.dp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = s.teamName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) // Smaller text
                Text(
                    text  = s.driverAcronyms.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${s.points}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) // Smaller text
                Text(text = "PTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        modifier             = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun rememberTeamColor(hex: String?): Color {
    return remember(hex) {
        if (hex.isNullOrBlank()) return@remember Color.Gray
        runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrElse { Color.Gray }
    }
}
