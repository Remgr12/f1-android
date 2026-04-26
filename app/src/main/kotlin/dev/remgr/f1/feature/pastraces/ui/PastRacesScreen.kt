package dev.remgr.f1.feature.pastraces.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remgr.f1.feature.pastraces.domain.model.Race
import dev.remgr.f1.feature.pastraces.domain.model.RaceResult
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PastRacesScreen(vm: PastRacesViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is PastRacesViewModel.UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is PastRacesViewModel.UiState.Error   -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            Arrangement.Center,
            Alignment.CenterHorizontally,
        ) {
            Text(s.message, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            TextButton(vm::retry) { Text("Retry") }
        }
        is PastRacesViewModel.UiState.Success -> RaceList(s.races)
    }
}

@Composable
private fun RaceList(races: List<Race>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(races, key = { it.sessionKey }) { RaceCard(it) }
    }
}

@Composable
private fun RaceCard(race: Race) {
    val (meetingTitle, sessionType) = splitMeetingAndSessionType(race.raceName)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(meetingTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    SessionTypeChip(sessionType)
                    Text(
                        "${race.location}, ${race.countryName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatDate(race.dateStart),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    race.circuitName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Text(
                text = "TOP 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
            Spacer(Modifier.height(6.dp))

            race.results.take(3).forEach { PodiumRow(it) }
        }
    }
}

@Composable
private fun PodiumRow(result: RaceResult) {
    val teamColor = remember(result.teamColour) {
        runCatching { Color(android.graphics.Color.parseColor("#${result.teamColour}")) }
            .getOrElse { Color.Gray }
    }
    val rank = "${result.position}."

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(rank, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(28.dp))
        Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
        Text(result.driverName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(result.teamName, style = MaterialTheme.typography.bodySmall, color = teamColor)
    }
}

@Composable
private fun SessionTypeChip(sessionType: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = sessionType,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
private fun formatDate(iso: String): String = runCatching {
    ZonedDateTime.parse(iso).format(dateFmt)
}.getOrElse { iso.take(10) }

private fun splitMeetingAndSessionType(value: String): Pair<String, String> {
    val separator = " — "
    val idx = value.lastIndexOf(separator)
    if (idx < 0) return value to "Race"
    return value.substring(0, idx) to value.substring(idx + separator.length)
}
