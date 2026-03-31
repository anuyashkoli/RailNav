package com.app.railnav.data

enum class TrainType(val displayName: String, val color: Long) {
    SLOW("Slow", 0xFF43A047),
    SEMI_FAST("Semi-Fast", 0xFFFB8C00),
    FAST("Fast", 0xFFE53935),
    AC_SPECIAL("AC Special", 0xFF1E88E5)
}

enum class TrainDirection(val displayName: String) {
    UP("↑ Towards Mumbai CSMT"),
    DOWN("↓ Towards Kasara / Karjat")
}

data class TrainSchedule(
    val trainNumber: String,
    val type: TrainType,
    val direction: TrainDirection,
    val platformAtThane: Int,
    /** Minutes since midnight, e.g. 6:10 AM = 370 */
    val departureMinutes: Int,
    val destination: String,
    /** Key intermediate stops in order */
    val via: List<String> = emptyList()
) {
    val departureTimeString: String
        get() {
            val normalised = departureMinutes % (24 * 60)
            return "%02d:%02d".format(normalised / 60, normalised % 60)
        }
}

/** Lightweight wrapper used by the Facilities bottom-sheet */
data class FacilityItem(
    val name: String,
    val description: String,
    val emoji: String,
    val nodeId: Int
)