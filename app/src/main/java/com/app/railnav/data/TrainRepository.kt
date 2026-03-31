package com.app.railnav.data

import java.util.Calendar

/**
 * Static dummy schedule for Thane station.
 *
 * Platform mapping used here (approximate, realistic for Thane):
 *   1  → Fast UP (towards CSMT)
 *   2  → Slow UP (towards CSMT)
 *   3  → Slow DOWN (towards Kasara/Karjat) – east-face
 *   4  → Slow DOWN (towards Kasara/Karjat) – west-face
 *   5  → Fast / Semi-Fast DOWN
 *   6  → Semi-Fast / Special DOWN
 *
 * Graph-node mapping – the level-0 (platform-level) stairway/escalator
 * nodes that sit mid-platform, giving the pathfinder a sensible target:
 *   Platform 1  → node 104
 *   Platform 2  → node 80
 *   Platform 3  → node 81
 *   Platform 4  → node 82
 *   Platform 5  → node 83
 *   Platform 6  → node 84
 *   Platform 7  → node 85
 *   Platform 8  → node 86
 *   Platform 9  → node 87
 *   Platform 10 → node 89
 */
object TrainRepository {

    // ── helpers ────────────────────────────────────────────────────────────
    private fun t(h: Int, m: Int) = h * 60 + m

    // ── platform → graph node_id ───────────────────────────────────────────
    val platformToNodeId: Map<Int, Int> = mapOf(
        1 to 104,
        2 to 80,
        3 to 81,
        4 to 82,
        5 to 83,
        6 to 84,
        7 to 85,
        8 to 86,
        9 to 87,
        10 to 89
    )

    // ── facilities ─────────────────────────────────────────────────────────
    /** These node_ids map directly to FACILITY nodes in nodes.geojson */
    val facilities: List<FacilityItem> = listOf(
        FacilityItem(
            name = "Toilet – West",
            description = "Near FOB junction, Platform 2 end",
            emoji = "🚻",
            nodeId = 14
        ),
        FacilityItem(
            name = "Toilet – West (Parking side)",
            description = "Adjacent to West 2-wheeler parking",
            emoji = "🚻",
            nodeId = 18
        ),
        FacilityItem(
            name = "Ticket Counter – West",
            description = "Main West booking office",
            emoji = "🎫",
            nodeId = 16
        ),
        FacilityItem(
            name = "Ticket Counter – East",
            description = "East side booking office",
            emoji = "🎫",
            nodeId = 19
        )
    )

    // ── all known stations (for search suggestions) ─────────────────────────
    val allStations: List<String> = listOf(
        // UP direction
        "CSMT", "Masjid", "Sandhurst Road", "Byculla", "Chinchpokli", "Currey Road",
        "Parel", "Dadar", "Matunga", "Sion", "Kurla", "Vidyavihar", "Ghatkopar",
        "Vikhroli", "Kanjurmarg", "Bhandup", "Nahur", "Mulund",
        // DOWN direction – Main Line
        "Kalwa", "Mumbra", "Diva", "Kopar", "Dombivli", "Thakurli", "Kalyan",
        "Shahad", "Ambivli", "Titwala", "Khadavali", "Vasind", "Asangaon",
        "Atgaon", "Khardi", "Kasara",
        // DOWN direction – Karjat branch (from Kalyan)
        "Vitthalwadi", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani",
        "Neral", "Bhivpuri Road", "Karjat"
    ).sorted()

    // ── dummy schedule ──────────────────────────────────────────────────────
    val schedule: List<TrainSchedule> = listOf(

        // ═══════════════════════════════════════════════════════════════════
        //  DOWN trains  (away from CSMT, towards Kasara / Karjat / Badlapur)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("98101", TrainType.SLOW,      TrainDirection.DOWN, 3, t(5,58),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind", "Asangaon")),
        TrainSchedule("98103", TrainType.FAST,      TrainDirection.DOWN, 5, t(6,10),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98105", TrainType.SLOW,      TrainDirection.DOWN, 4, t(6,18),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98107", TrainType.SLOW,      TrainDirection.DOWN, 3, t(6,25),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath")),
        TrainSchedule("98109", TrainType.FAST,      TrainDirection.DOWN, 6, t(6,40),  "Kasara",
            listOf("Kalyan", "Titwala", "Vasind")),
        TrainSchedule("98111", TrainType.SLOW,      TrainDirection.DOWN, 4, t(6,52),  "Titwala",
            listOf("Dombivli", "Kalyan", "Shahad", "Ambivli")),
        TrainSchedule("98113", TrainType.SLOW,      TrainDirection.DOWN, 3, t(7,5),   "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98115", TrainType.FAST,      TrainDirection.DOWN, 5, t(7,15),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98117", TrainType.SLOW,      TrainDirection.DOWN, 4, t(7,28),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Khadavali", "Vasind")),
        TrainSchedule("98119", TrainType.SLOW,      TrainDirection.DOWN, 3, t(7,42),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath")),
        TrainSchedule("98121", TrainType.SEMI_FAST, TrainDirection.DOWN, 6, t(7,55),  "Kasara",
            listOf("Kalyan", "Titwala", "Vasind")),
        TrainSchedule("98123", TrainType.SLOW,      TrainDirection.DOWN, 4, t(8,8),   "Karjat",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98125", TrainType.FAST,      TrainDirection.DOWN, 5, t(8,22),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98127", TrainType.SLOW,      TrainDirection.DOWN, 3, t(8,35),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind", "Asangaon")),
        TrainSchedule("98129", TrainType.SLOW,      TrainDirection.DOWN, 4, t(8,50),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98131", TrainType.FAST,      TrainDirection.DOWN, 6, t(9,5),   "Kasara",
            listOf("Kalyan", "Titwala")),
        TrainSchedule("98133", TrainType.SLOW,      TrainDirection.DOWN, 3, t(9,20),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98135", TrainType.SLOW,      TrainDirection.DOWN, 4, t(9,35),  "Titwala",
            listOf("Dombivli", "Kalyan", "Shahad", "Ambivli")),
        TrainSchedule("98137", TrainType.FAST,      TrainDirection.DOWN, 5, t(10,0),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98139", TrainType.SLOW,      TrainDirection.DOWN, 3, t(10,25), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind")),
        TrainSchedule("98141", TrainType.SLOW,      TrainDirection.DOWN, 4, t(11,10), "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98143", TrainType.FAST,      TrainDirection.DOWN, 5, t(12,15), "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98145", TrainType.SLOW,      TrainDirection.DOWN, 3, t(13,20), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),
        TrainSchedule("98147", TrainType.SLOW,      TrainDirection.DOWN, 4, t(14,45), "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur")),
        TrainSchedule("98149", TrainType.FAST,      TrainDirection.DOWN, 6, t(16,10), "Kasara",
            listOf("Kalyan", "Titwala")),
        TrainSchedule("98151", TrainType.SLOW,      TrainDirection.DOWN, 3, t(17,5),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98153", TrainType.FAST,      TrainDirection.DOWN, 5, t(17,30), "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98155", TrainType.SLOW,      TrainDirection.DOWN, 4, t(18,15), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),
        TrainSchedule("98157", TrainType.SLOW,      TrainDirection.DOWN, 3, t(19,0),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98159", TrainType.FAST,      TrainDirection.DOWN, 6, t(19,45), "Kasara",
            listOf("Kalyan")),
        TrainSchedule("98161", TrainType.SLOW,      TrainDirection.DOWN, 4, t(20,30), "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98163", TrainType.SLOW,      TrainDirection.DOWN, 3, t(21,15), "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98165", TrainType.SLOW,      TrainDirection.DOWN, 4, t(22,0),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),

        // ═══════════════════════════════════════════════════════════════════
        //  UP trains  (towards CSMT / Mumbai)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("97102", TrainType.SLOW,      TrainDirection.UP, 2, t(6,5),   "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97104", TrainType.FAST,      TrainDirection.UP, 1, t(6,20),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97106", TrainType.SLOW,      TrainDirection.UP, 2, t(6,45),  "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97108", TrainType.FAST,      TrainDirection.UP, 1, t(7,10),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97110", TrainType.SLOW,      TrainDirection.UP, 2, t(7,30),  "Dadar",
            listOf("Mulund", "Nahur", "Ghatkopar", "Kurla", "Sion")),
        TrainSchedule("97112", TrainType.FAST,      TrainDirection.UP, 1, t(7,55),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97114", TrainType.SLOW,      TrainDirection.UP, 2, t(8,15),  "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97116", TrainType.SEMI_FAST, TrainDirection.UP, 1, t(8,40),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97118", TrainType.SLOW,      TrainDirection.UP, 2, t(9,10),  "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97120", TrainType.FAST,      TrainDirection.UP, 1, t(9,45),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97122", TrainType.SLOW,      TrainDirection.UP, 2, t(10,20), "Dadar",
            listOf("Mulund", "Ghatkopar", "Kurla")),
        TrainSchedule("97124", TrainType.FAST,      TrainDirection.UP, 1, t(11,0),  "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97126", TrainType.SLOW,      TrainDirection.UP, 2, t(12,10), "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97128", TrainType.FAST,      TrainDirection.UP, 1, t(13,30), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97130", TrainType.SLOW,      TrainDirection.UP, 2, t(14,50), "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97132", TrainType.FAST,      TrainDirection.UP, 1, t(16,20), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97134", TrainType.SLOW,      TrainDirection.UP, 2, t(17,40), "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97136", TrainType.FAST,      TrainDirection.UP, 1, t(18,50), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97138", TrainType.SLOW,      TrainDirection.UP, 2, t(19,30), "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97140", TrainType.FAST,      TrainDirection.UP, 1, t(20,15), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97142", TrainType.SLOW,      TrainDirection.UP, 2, t(21,0),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97144", TrainType.SLOW,      TrainDirection.UP, 2, t(22,10), "Dadar",
            listOf("Mulund", "Ghatkopar", "Kurla"))
    )

    // ── public API ──────────────────────────────────────────────────────────

    /** Fuzzy-free: returns stations whose name contains [query] (case-insensitive). */
    fun searchDestinations(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return allStations.filter { it.lowercase().contains(q) }.take(8)
    }

    /**
     * Returns all trains that serve [destination] – either as the final stop
     * or as a listed via station.
     */
    fun getTrainsForDestination(destination: String): List<TrainSchedule> {
        val dest = destination.trim().lowercase()
        return schedule.filter { train ->
            train.destination.lowercase() == dest ||
                    train.via.any { it.lowercase() == dest }
        }.sortedBy { it.departureMinutes }
    }

    /**
     * Returns up to [count] upcoming trains for [destination], wrapping
     * around midnight when needed so there's always something to show.
     */
    fun getUpcomingTrains(
        destination: String,
        currentMinutes: Int = currentMinutes(),
        count: Int = 6
    ): List<TrainSchedule> {
        val all = getTrainsForDestination(destination)
        if (all.isEmpty()) return emptyList()
        val upcoming = all.filter { it.departureMinutes >= currentMinutes }
        return if (upcoming.size >= count) {
            upcoming.take(count)
        } else {
            // Wrap: show beginning of next day's schedule to fill the list
            (upcoming + all.take(count - upcoming.size)).take(count)
        }
    }

    /** Returns the graph node_id for the mid-platform stairway of [platformNumber]. */
    fun getPlatformNodeId(platformNumber: Int): Int? = platformToNodeId[platformNumber]

    /** Current time as minutes since midnight. */
    fun currentMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /** Human-readable "HH:MM" for a minutes-since-midnight value. */
    fun formatMinutes(minutes: Int): String {
        val n = minutes % (24 * 60)
        return "%02d:%02d".format(n / 60, n % 60)
    }
}