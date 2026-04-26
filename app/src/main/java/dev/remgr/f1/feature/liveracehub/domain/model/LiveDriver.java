package dev.remgr.f1.feature.liveracehub.domain.model;

public record LiveDriver(
        int position,
        int driverNumber,
        String nameAcronym,
        String teamName,
        String teamColour,
        Double gapToLeader,
        Double interval,
        int currentLap,
        boolean inPit
) {}
