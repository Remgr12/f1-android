package dev.remgr.f1.feature.racedetail.domain.model;

public record LapData(
        int driverNumber,
        String nameAcronym,
        String teamColour,
        int lapNumber,
        Double lapDuration,
        boolean isPitOutLap,
        Double sector1,
        Double sector2,
        Double sector3
) {}
