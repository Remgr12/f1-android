package dev.remgr.f1.feature.trackmap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remgr.f1.feature.trackmap.domain.model.TrackPosition
import dev.remgr.f1.feature.trackmap.domain.TrackSessionOption
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMapScreen(vm: TrackMapViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val options by vm.trackOptions.collectAsStateWithLifecycle()
    val selectedSessionKey by vm.selectedSessionKey.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        TrackSessionSelector(
            options = options,
            selectedSessionKey = selectedSessionKey,
            onSelect = vm::selectSession,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.trackPoints.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Waiting for telemetry…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                TrackCanvas(
                    trackPoints  = state.trackPoints,
                    carPositions = state.carPositions,
                    modifier     = Modifier.fillMaxSize(),
                )
                CarLegend(
                    cars     = state.carPositions,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSessionSelector(
    options: List<TrackSessionOption>,
    selectedSessionKey: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.sessionKey == selectedSessionKey } ?: options.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedOption?.label ?: "Loading tracks…",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Track / Race") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(option.sessionKey)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Canvas renderer ───────────────────────────────────────────────────────────

@Composable
fun TrackCanvas(
    trackPoints: List<Offset>,
    carPositions: List<TrackPosition>,
    modifier: Modifier = Modifier,
) {
    // Pre-compute bounds — stable unless the track point set changes.
    val bounds = remember(trackPoints) {
        if (trackPoints.isEmpty()) return@remember null
        TrackBounds(
            minX = trackPoints.minOf { it.x },
            maxX = trackPoints.maxOf { it.x },
            minY = trackPoints.minOf { it.y },
            maxY = trackPoints.maxOf { it.y },
        )
    } ?: return

    // Preserve telemetry/path order and thin dense sets for rendering performance.
    val orderedTrack = remember(trackPoints) {
        val step = when {
            trackPoints.size > 16_000 -> 12
            trackPoints.size > 10_000 -> 8
            trackPoints.size > 6_000 -> 6
            trackPoints.size > 3_000 -> 4
            else -> 1
        }
        trackPoints.filterIndexed { index, _ -> index % step == 0 }
    }

    Canvas(modifier = modifier) {
        val pad = 40.dp.toPx()
        val availableW = (size.width - pad * 2).coerceAtLeast(1f)
        val availableH = (size.height - pad * 2).coerceAtLeast(1f)
        val spanX = (bounds.maxX - bounds.minX).coerceAtLeast(1f)
        val spanY = (bounds.maxY - bounds.minY).coerceAtLeast(1f)
        val scale = min(availableW / spanX, availableH / spanY)
        val contentW = spanX * scale
        val contentH = spanY * scale
        val offsetX = (size.width - contentW) / 2f
        val offsetY = (size.height - contentH) / 2f

        fun Offset.normalise(): Offset = Offset(
            x = offsetX + (x - bounds.minX) * scale,
            // Flip Y — OpenF1 Y increases upward, Canvas Y increases downward.
            y = offsetY + (spanY - (y - bounds.minY)) * scale,
        )

        // ── Track outline ─────────────────────────────────────────────────────
        if (orderedTrack.size > 3) {
            val path = Path().apply {
                val first = orderedTrack.first().normalise()
                moveTo(first.x, first.y)
                orderedTrack.drop(1).forEach { pt -> with(pt.normalise()) { lineTo(x, y) } }
            }
            // Outer kerb (dark grey, wide)
            drawPath(path, Color(0xFF2A2A2A), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Asphalt surface
            drawPath(path, Color(0xFF444444), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Centre line
            drawPath(path, Color.White.copy(alpha = 0.15f), style = Stroke(width = 2.dp.toPx()))
        }

        // ── Cars ──────────────────────────────────────────────────────────────
        carPositions.forEach { car ->
            val carColor = runCatching {
                Color(android.graphics.Color.parseColor("#${car.teamColour}"))
            }.getOrElse { Color.White }

            val pos = Offset(car.x.toFloat(), car.y.toFloat()).normalise()

            // Glow ring
            drawCircle(carColor.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = pos)
            // Car dot
            drawCircle(carColor, radius = 7.dp.toPx(), center = pos)
            // White centre dot for contrast
            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pos)

            // Driver acronym label
            drawContext.canvas.nativeCanvas.drawText(
                car.nameAcronym,
                pos.x + 10.dp.toPx(),
                pos.y - 10.dp.toPx(),
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.WHITE
                    textSize  = 10.dp.toPx()
                    isFakeBoldText = true
                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                },
            )
        }
    }
}

// ── Legend overlay ────────────────────────────────────────────────────────────

@Composable
private fun CarLegend(cars: List<TrackPosition>, modifier: Modifier = Modifier) {
    if (cars.isEmpty()) return
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        color     = Color(0xCC1A1A1A),
        tonalElevation = 4.dp,
    ) {
        LazyColumn(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(cars.sortedBy { it.driverNumber }) { car ->
                val color = remember(car.teamColour) {
                    runCatching { Color(android.graphics.Color.parseColor("#${car.teamColour}")) }
                        .getOrElse { Color.Gray }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(
                        text       = car.nameAcronym,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
            }
        }
    }
}

// ── Internal helpers ──────────────────────────────────────────────────────────

private data class TrackBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)
