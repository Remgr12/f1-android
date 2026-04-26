package dev.remgr.f1.feature.trackmap.domain

import androidx.compose.ui.geometry.Offset
import dev.remgr.f1.feature.trackmap.domain.model.TrackPosition
import kotlinx.coroutines.flow.Flow

data class TrackMapState(
    // Track outline: all unique (x,y) coordinates accumulated from the session.
    val trackPoints: List<Offset>,
    // Latest car positions from /location endpoint.
    val carPositions: List<TrackPosition>,
)

data class TrackSessionOption(
    val sessionKey: Int?,
    val label: String,
)

interface TrackMapRepository {
    suspend fun getTrackOptions(): List<TrackSessionOption>
    fun getTrackMapState(sessionKey: Int?): Flow<TrackMapState>
}
