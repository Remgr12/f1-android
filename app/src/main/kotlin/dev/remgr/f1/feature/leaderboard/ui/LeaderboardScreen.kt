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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding

@Composable
fun LeaderboardScreen(vm: LeaderboardViewModel = hiltViewModel()) {
    val state       by vm.uiState.collectAsStateWithLifecycle()
    val selectedTab = vm.selectedTab

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            listOf("Drivers", "Constructors").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick  = { vm.selectTab(idx) },
                    text     = { Text(label, modifier = Modifier.padding(vertical = 14.dp)) },
                )
            }
        }

        when (val s = state) {
            is LeaderboardViewModel.UiState.Loading -> LoadingBox()
            is LeaderboardViewModel.UiState.Error   -> ErrorBox(s.message, vm::retry)
            is LeaderboardViewModel.UiState.Success -> {
                if (selectedTab == 0) DriverList(s.drivers)
                else ConstructorList(s.constructors)
            }
        }
    }
}

@Composable
private fun DriverList(drivers: List<DriverStanding>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            modifier             = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Position badge
            Box(
                modifier        = Modifier.size(40.dp).clip(CircleShape).background(teamColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "${s.position}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = teamColor,
                )
            }

            AsyncImage(
                model            = s.headshotUrl,
                contentDescription = s.fullName,
                modifier         = Modifier.size(48.dp).clip(CircleShape),
                contentScale     = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = s.nameAcronym, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = s.teamName, style = MaterialTheme.typography.bodySmall, color = teamColor)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${s.points}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "PTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Team color stripe
            Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
        }
    }
}

@Composable
private fun ConstructorList(constructors: List<ConstructorStanding>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(constructors, key = { it.teamName }) { ConstructorCard(it) }
    }
}

@Composable
private fun ConstructorCard(s: ConstructorStanding) {
    val teamColor = rememberTeamColor(s.teamColour)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text       = "${s.position}",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = teamColor,
                modifier   = Modifier.width(32.dp),
                textAlign  = TextAlign.Center,
            )

            HorizontalDivider(
                modifier  = Modifier.height(48.dp).width(4.dp).clip(RoundedCornerShape(2.dp)),
                color     = teamColor,
                thickness = 4.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = s.teamName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text  = s.driverAcronyms.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${s.points}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
