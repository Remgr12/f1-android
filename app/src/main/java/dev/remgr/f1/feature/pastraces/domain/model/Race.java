package dev.remgr.f1.feature.pastraces.domain.model;

import java.util.List;

public record Race(
        int meetingKey,
        int sessionKey,
        String raceName,
        String location,
        String countryName,
        String circuitName,
        String dateStart,
        List<RaceResult> results
) {}
