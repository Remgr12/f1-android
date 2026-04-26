package dev.remgr.f1.feature.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.remgr.f1.core.util.F1Images

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructorDetailScreen(
    onBack: () -> Unit,
    vm: ConstructorDetailViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? ConstructorDetailViewModel.UiState.Success)?.detail?.teamName ?: "Constructor"
                    Text(title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is ConstructorDetailViewModel.UiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding), Alignment.Center
            ) { CircularProgressIndicator() }

            is ConstructorDetailViewModel.UiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally
            ) {
                Text(s.message, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TextButton(vm::retry) { Text("Retry") }
            }

            is ConstructorDetailViewModel.UiState.Success -> ConstructorDetailContent(
                detail = s.detail,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ConstructorDetailContent(detail: ConstructorDetailState, modifier: Modifier = Modifier) {
    val teamColor = rememberTeamColor(detail.teamColour)
    val logoUrl = F1Images.getConstructorLogoUrl(detail.teamName)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (logoUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = detail.teamName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                            .background(teamColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = detail.teamName.take(3).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = teamColor,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(detail.teamName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(teamColor)
                )
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "SEASON HISTORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        items(detail.seasons) { season ->
            ConstructorSeasonRow(season = season, teamColour = detail.teamColour)
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ConstructorSeasonRow(season: ConstructorSeasonRow, teamColour: String) {
    val teamColor = rememberTeamColor(teamColour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${season.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(52.dp),
        )
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(teamColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "P${season.position}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = teamColor,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = season.driverAcronyms.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${season.points} pts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (season.wins > 0) {
                Text("${season.wins} win${if (season.wins > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.surfaceVariant)
}
