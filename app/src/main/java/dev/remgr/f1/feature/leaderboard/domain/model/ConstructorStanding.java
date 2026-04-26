package dev.remgr.f1.feature.leaderboard.domain.model;

import java.util.List;

public record ConstructorStanding(
        int position,
        String teamName,
        String teamColour,
        int points,
        int wins,
        List<String> driverAcronyms
) implements Comparable<ConstructorStanding> {

    @Override
    public int compareTo(ConstructorStanding other) {
        return Integer.compare(other.points, this.points);
    }
}
