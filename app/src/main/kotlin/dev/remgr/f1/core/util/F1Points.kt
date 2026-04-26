package dev.remgr.f1.core.util

object F1Points {
    private val table = mapOf(
        1 to 25, 2 to 18, 3 to 15, 4 to 12, 5 to 10,
        6 to 8,  7 to 6,  8 to 4,  9 to 2,  10 to 1,
    )

    fun forPosition(position: Int): Int = table[position] ?: 0

    // Sprint points (half distance, half points)
    private val sprintTable = mapOf(
        1 to 8, 2 to 7, 3 to 6, 4 to 5, 5 to 4,
        6 to 3, 7 to 2, 8 to 1,
    )

    fun forSprintPosition(position: Int): Int = sprintTable[position] ?: 0
}
