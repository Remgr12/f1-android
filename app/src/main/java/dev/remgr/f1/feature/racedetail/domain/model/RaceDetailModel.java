package dev.remgr.f1.feature.racedetail.domain.model;

import dev.remgr.f1.feature.pastraces.domain.model.RaceResult;
import java.util.List;

public record RaceDetailModel(
        int meetingKey,
        int sessionKey,
        String raceName,
        String location,
        String circuitName,
        List<RaceResult> results,
        List<LapData> laps,
        List<PitStop> pitStops,
        int totalLaps
) {}
