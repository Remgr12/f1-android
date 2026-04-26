package dev.remgr.f1.feature.racedetail.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remgr.f1.feature.pastraces.domain.model.RaceResult
import dev.remgr.f1.feature.racedetail.domain.model.LapData
import dev.remgr.f1.feature.racedetail.domain.model.PitStop
import dev.remgr.f1.feature.racedetail.domain.model.RaceDetailModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    onBack: () -> Unit,
    vm: RaceDetailViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val tab   = vm.selectedTab

    Scaffold(
        topBar = {
            TopAppBar(
                title       = { Text(if (state is RaceDetailViewModel.UiState.Success) (state as RaceDetailViewModel.UiState.Success).detail.raceName else "Race Detail") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab) {
                listOf("Results", "Lap Chart", "Pit Stops", "Sectors").forEachIndexed { idx, label ->
                    Tab(selected = tab == idx, onClick = { vm.selectTab(idx) },
                        text = { Text(label, modifier = Modifier.padding(vertical = 14.dp)) })
                }
            }

            when (val s = state) {
                is RaceDetailViewModel.UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is RaceDetailViewModel.UiState.Error   -> Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(s.message, textAlign = TextAlign.Center)
                    TextButton(vm::retry) { Text("Retry") }
                }
                is RaceDetailViewModel.UiState.Success -> when (tab) {
                    0 -> ResultsList(s.detail.results)
                    1 -> LapChartCanvas(s.detail.laps, modifier = Modifier.fillMaxSize().padding(8.dp))
                    2 -> PitTimelineCanvas(s.detail.results, s.detail.pitStops, s.detail.totalLaps,
                                modifier = Modifier.fillMaxWidth().padding(8.dp))
                    3 -> SectorTimesList(s.detail.laps)
                }
            }
        }
    }
}

// ── Results tab ───────────────────────────────────────────────────────────────

@Composable
private fun ResultsList(results: List<RaceResult>) {
    LazyColumn {
        items(results, key = { it.driverNumber }) { result ->
            val teamColor = remember(result.teamColour) {
                runCatching { Color(android.graphics.Color.parseColor("#${result.teamColour}")) }
                    .getOrElse { Color.Gray }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${result.position}", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
                Column(Modifier.weight(1f)) {
                    Text(result.nameAcronym, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(result.teamName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${result.points} pts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Sector Times Tab ─────────────────────────────────────────────────────────

@Composable
private fun SectorTimesList(laps: List<LapData>) {
    // Find best lap for each driver
    val bestLapsByDriver = remember(laps) {
        laps.filter { it.lapDuration != null && it.lapDuration > 0 && !it.isPitOutLap }
            .groupBy { it.driverNumber }
            .mapNotNull { (_, driverLaps) -> driverLaps.minByOrNull { it.lapDuration!! } }
            .sortedBy { it.lapDuration!! }
    }

    LazyColumn {
        items(bestLapsByDriver, key = { it.driverNumber }) { lap ->
            val teamColor = remember(lap.teamColour) {
                runCatching { Color(android.graphics.Color.parseColor("#${lap.teamColour}")) }
                    .getOrElse { Color.Gray }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))
                Column(Modifier.weight(1f)) {
                    Text(lap.nameAcronym, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val s1 = lap.sector1?.let { String.format(Locale.US, "%.3f", it) } ?: "--"
                        val s2 = lap.sector2?.let { String.format(Locale.US, "%.3f", it) } ?: "--"
                        val s3 = lap.sector3?.let { String.format(Locale.US, "%.3f", it) } ?: "--"
                        Text("S1: $s1", style = MaterialTheme.typography.bodySmall)
                        Text("S2: $s2", style = MaterialTheme.typography.bodySmall)
                        Text("S3: $s3", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(String.format(Locale.US, "%.3fs", lap.lapDuration!!), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Lap Chart — Canvas ────────────────────────────────────────────────────────

@Composable
fun LapChartCanvas(laps: List<LapData>, modifier: Modifier = Modifier) {
    val byDriver = remember(laps) { laps.groupBy { it.driverNumber } }

    // Filter to valid, non-outlap times (<200 s keeps out red flags / SC restarts that inflate times)
    val allTimes = laps.mapNotNull { it.lapDuration }.filter { it > 0 && it < 200 }
    if (allTimes.isEmpty()) {
        Box(modifier, Alignment.Center) { Text("No lap data available") }
        return
    }

    val minTime = allTimes.min()
    val maxTime = allTimes.max()
    val maxLap  = laps.maxOfOrNull { it.lapNumber } ?: return

    Canvas(modifier = modifier) {
        val padL = 12.dp.toPx(); val padR = 12.dp.toPx()
        val padT = 12.dp.toPx(); val padB = 24.dp.toPx()
        val w    = size.width - padL - padR
        val h    = size.height - padT - padB

        fun lapX(lap: Int)   = padL + lap.toFloat() / maxLap * w
        fun timeY(t: Double) = padT + (1f - ((t - minTime) / (maxTime - minTime)).toFloat()) * h

        // Subtle grid
        (0..4).forEach { i ->
            val y = padT + i.toFloat() / 4 * h
            drawLine(Color.Gray.copy(alpha = 0.15f), Offset(padL, y), Offset(padL + w, y))
        }

        byDriver.forEach { (_, driverLaps) ->
            val clean = driverLaps.filter { it.lapDuration != null && !it.isPitOutLap }
                .sortedBy { it.lapNumber }
            if (clean.size < 2) return@forEach

            val teamColor = runCatching {
                Color(android.graphics.Color.parseColor("#${clean.first().teamColour}"))
            }.getOrElse { Color.Gray }

            var prevLap: LapData? = null
            val path = Path().apply {
                var first = true
                clean.forEach { lap ->
                    val yPos = timeY(lap.lapDuration!!)
                    if (first) {
                        moveTo(lapX(lap.lapNumber), yPos)
                        first = false
                    } else if (lap.lapNumber - prevLap!!.lapNumber == 1) {
                        lineTo(lapX(lap.lapNumber), yPos)
                    } else {
                        moveTo(lapX(lap.lapNumber), yPos)
                    }
                    prevLap = lap
                }
            }
            drawPath(path, teamColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

            // Dot at last point
            val last = clean.last()
            drawCircle(teamColor, 3.dp.toPx(), Offset(lapX(last.lapNumber), timeY(last.lapDuration!!)))
        }
    }
}

// ── Pit Timeline — Canvas ─────────────────────────────────────────────────────

@Composable
fun PitTimelineCanvas(
    results: List<RaceResult>,
    pitStops: List<PitStop>,
    totalLaps: Int,
    modifier: Modifier = Modifier,
) {
    val pitsByDriver = remember(pitStops) { pitStops.groupBy { it.driverNumber } }
    val rows         = results.take(20)
    val rowHeightDp  = 28.dp

    Canvas(modifier = modifier.height(rowHeightDp * rows.size)) {
        val labelW   = 42.dp.toPx()
        val barH     = 14.dp.toPx()
        val rowH     = rowHeightDp.toPx()
        val barW     = size.width - labelW - 8.dp.toPx()
        val effectiveLaps = totalLaps.coerceAtLeast(1)

        rows.forEachIndexed { idx, result ->
            val y = idx * rowH + (rowH - barH) / 2f
            val teamColor = runCatching {
                Color(android.graphics.Color.parseColor("#${result.teamColour}"))
            }.getOrElse { Color.Gray }

            // Background tray
            drawRoundRect(
                teamColor.copy(alpha = 0.12f),
                topLeft = Offset(labelW, y),
                size    = Size(barW, barH),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            // Stint segments
            val pits   = pitsByDriver[result.driverNumber] ?: emptyList()
            val bounds = listOf(0) + pits.map { it.lapNumber } + listOf(effectiveLaps)

            bounds.zipWithNext().forEachIndexed { stintIdx, (start, end) ->
                val x1 = labelW + (start.toFloat() / effectiveLaps) * barW
                val x2 = labelW + (end.toFloat()   / effectiveLaps) * barW
                val alpha = (0.45f + stintIdx * 0.15f).coerceAtMost(0.9f)
                drawRoundRect(
                    teamColor.copy(alpha = alpha),
                    topLeft  = Offset(x1, y),
                    size     = Size((x2 - x1).coerceAtLeast(1f), barH),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }

            // Pit markers (white vertical bars)
            pits.forEach { pit ->
                val x = labelW + (pit.lapNumber.toFloat() / effectiveLaps) * barW
                drawLine(Color.White.copy(alpha = 0.9f), Offset(x, y), Offset(x, y + barH), 2.dp.toPx())
            }

            // Driver acronym
            drawContext.canvas.nativeCanvas.drawText(
                result.nameAcronym,
                4.dp.toPx(),
                y + barH - 2.dp.toPx(),
                android.graphics.Paint().apply {
                    color     = teamColor.toArgb()
                    textSize  = 10.dp.toPx()
                    isFakeBoldText = true
                },
            )
        }
    }
}
