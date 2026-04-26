package dev.remgr.f1.feature.pastraces.domain.model;

public record RaceResult(
        int position,
        int driverNumber,
        String driverName,
        String nameAcronym,
        String teamName,
        String teamColour,
        int points
) {}
