package dev.remgr.f1.feature.racedetail.domain.model;

public record PitStop(
        int driverNumber,
        String nameAcronym,
        int lapNumber,
        Double duration
) {}
