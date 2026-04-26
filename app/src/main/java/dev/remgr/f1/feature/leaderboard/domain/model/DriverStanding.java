package dev.remgr.f1.feature.leaderboard.domain.model;

/// Immutable domain model — Java 26 record.
public record DriverStanding(
        int position,
        int driverNumber,
        String fullName,
        String nameAcronym,
        String teamName,
        String teamColour,
        String headshotUrl,
        int points,
        int wins
) implements Comparable<DriverStanding> {

    @Override
    public int compareTo(DriverStanding other) {
        int cmp = Integer.compare(this.points, other.points);
        if (cmp != 0) return -cmp; // descending points
        return Integer.compare(this.wins, other.wins) * -1;
    }
}
