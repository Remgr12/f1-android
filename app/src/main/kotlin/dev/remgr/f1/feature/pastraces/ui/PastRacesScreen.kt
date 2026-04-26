package dev.remgr.f1.feature.pastraces.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remgr.f1.feature.pastraces.domain.model.Race
import dev.remgr.f1.feature.pastraces.domain.model.RaceResult
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource

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
        is PastRacesViewModel.UiState.Success -> YearList(s.years)
    }
}

@Composable
private fun YearList(years: List<YearRaces>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        years.forEach { yearRaces ->
            item(key = yearRaces.year) {
                YearDrawer(yearRaces)
            }
        }
    }
}

@Composable
private fun YearDrawer(yearRaces: YearRaces) {
    val currentYear = remember { java.time.Year.now().value }
    var expanded by rememberSaveable { mutableStateOf(yearRaces.year == currentYear) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = !expanded },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${yearRaces.year} Season",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                yearRaces.meetings.forEach { meeting ->
                    MeetingCard(meeting)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MeetingCard(meeting: MeetingRaces) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(meeting.meetingName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${meeting.location}, ${meeting.countryName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatDateRange(meeting.sessions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        meeting.circuitName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))

                    meeting.sessions.forEachIndexed { index, session ->
                        val (_, sessionType) = splitMeetingAndSessionType(session.raceName())
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SessionTypeChip(sessionType)
                                Text(
                                    formatDate(session.dateStart()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            
                            if (session.results().isNotEmpty()) {
                                Text(
                                    text = "TOP 3 RESULTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                                )
                                Spacer(Modifier.height(4.dp))
                                session.results().take(3).forEach { PodiumRow(it) }
                            } else {
                                Text(
                                    text = "No results available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            
                            if (index < meeting.sessions.size - 1) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumRow(result: RaceResult) {
    val teamColor = remember(result.teamColour()) {
        runCatching { Color(android.graphics.Color.parseColor("#${result.teamColour()}")) }
            .getOrElse { Color.Gray }
    }
    val rank = "${result.position()}."

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(rank, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(28.dp))
        Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
        Text(result.driverName(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(result.teamName(), style = MaterialTheme.typography.bodySmall, color = teamColor)
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

private fun formatDateRange(sessions: List<Race>): String {
    if (sessions.isEmpty()) return ""
    val firstDate = formatDate(sessions.first().dateStart())
    val lastDate = formatDate(sessions.last().dateStart())
    return if (firstDate == lastDate) firstDate else "$firstDate - $lastDate"
}

private fun splitMeetingAndSessionType(value: String): Pair<String, String> {
    val separator = " — "
    val idx = value.lastIndexOf(separator)
    if (idx < 0) return value to "Race"
    return value.substring(0, idx) to value.substring(idx + separator.length)
}
