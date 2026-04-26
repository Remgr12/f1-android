package dev.remgr.f1.feature.liveracehub.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remgr.f1.feature.liveracehub.domain.RaceControlMessage
import dev.remgr.f1.feature.liveracehub.domain.model.LiveDriver

@Composable
fun LiveRaceHubScreen(vm: LiveRaceHubViewModel = hiltViewModel()) {
    val standings by vm.liveStandings.collectAsStateWithLifecycle()
    val messages  by vm.raceControlMessages.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LiveHeader()
        messages.lastOrNull()?.let { FlagBanner(it) }
        if (standings.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Waiting for live session…", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                items(standings, key = { it.driverNumber }) { LiveDriverRow(it) }
            }
        }
    }
}

@Composable
private fun LiveHeader() {
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(10.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = pulse))
        )
        Text(
            text       = "LIVE",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onErrorContainer,
            letterSpacing = 3.sp,
        )
    }
}

@Composable
private fun FlagBanner(msg: RaceControlMessage) {
    val (bg, fg) = when (msg.flag) {
        "RED"                              -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "YELLOW", "DOUBLE YELLOW"          -> Color(0xFFFDD835) to Color(0xFF1A1A1A)
        "SAFETY CAR", "VIRTUAL SAFETY CAR" -> Color(0xFFFF9800) to Color.Black
        else                               -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Flag, contentDescription = null, tint = fg)
        Text(msg.message, style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LiveDriverRow(driver: LiveDriver) {
    val teamColor = remember(driver.teamColour) {
        runCatching { Color(android.graphics.Color.parseColor("#${driver.teamColour}")) }
            .getOrElse { Color.Gray }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text      = "${driver.position}".padStart(2),
            style     = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier  = Modifier.width(28.dp),
        )

        Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))

        Column(modifier = Modifier.weight(1f)) {
            Text(driver.nameAcronym, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(driver.teamName,   style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = formatGap(driver.gapToLeader, driver.position),
                style      = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            if (driver.currentLap > 0) {
                Text(
                    text  = "LAP ${driver.currentLap}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (driver.inPit) {
            Text(
                text  = "PIT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }

    // Thin separator
    Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

private fun formatGap(gap: Double?, position: Int): String = when {
    position == 1 -> "LEADER"
    gap == null   -> "—"
    gap >= 60     -> "+${(gap / 60).toInt()}L"
    else          -> "+%.3f".format(gap)
}
