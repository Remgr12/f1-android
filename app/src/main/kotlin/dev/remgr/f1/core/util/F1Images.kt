package dev.remgr.f1.core.util

object F1Images {
    /**
     * Map of constructor team names to their logo URLs.
     */
    private val CONSTRUCTOR_LOGOS = mapOf(
        "Red Bull Racing" to "https://media.formula1.com/content/dam/fom-website/teams/2024/red-bull-racing-logo.png",
        "Ferrari" to "https://media.formula1.com/content/dam/fom-website/teams/2024/ferrari-logo.png",
        "Mercedes" to "https://media.formula1.com/content/dam/fom-website/teams/2024/mercedes-logo.png",
        "McLaren" to "https://media.formula1.com/content/dam/fom-website/teams/2024/mclaren-logo.png",
        "Aston Martin" to "https://media.formula1.com/content/dam/fom-website/teams/2024/aston-martin-logo.png",
        "Alpine" to "https://media.formula1.com/content/dam/fom-website/teams/2024/alpine-logo.png",
        "Williams" to "https://media.formula1.com/content/dam/fom-website/teams/2024/williams-logo.png",
        "RB" to "https://media.formula1.com/content/dam/fom-website/teams/2024/rb-logo.png",
        "Sauber" to "https://media.formula1.com/content/dam/fom-website/teams/2024/sauber-logo.png",
        "Haas F1 Team" to "https://media.formula1.com/content/dam/fom-website/teams/2024/haas-f1-team-logo.png"
    )

    fun getConstructorLogoUrl(teamName: String?): String? {
        if (teamName == null) return null
        return CONSTRUCTOR_LOGOS[teamName] ?: CONSTRUCTOR_LOGOS.entries.find { 
            teamName.contains(it.key, ignoreCase = true) 
        }?.value
    }
}
