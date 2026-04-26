package dev.remgr.f1.feature.trackmap.domain.model;

public record TrackPosition(
        int driverNumber,
        String nameAcronym,
        String teamColour,
        int x,
        int y,
        int z
) {}
