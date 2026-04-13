package com.app.railnav.data

import java.util.Calendar

/**
 * Static schedule for Thane station.
 *
 * Correct platform mapping for Thane:
 *   1   → Slow UP (towards CSMT) – Thane-start trains
 *   2   → Slow / Semi-Fast DOWN (towards Kasara/Karjat)
 *   3   → Slow DOWN (towards Kasara/Karjat) – east-face – Thane-start trains
 *   4   → Slow / Semi-Fast UP (towards CSMT) – west-face
 *   5   → Fast / Semi-Fast DOWN
 *   6   → Fast / Semi-Fast UP
 *   7   → Outstation – Fast UP (towards CSMT) – express / long-route trains
 *   8   → Outstation (towards CSMT)
 *   9   → Slow DOWN (towards Panvel / Vashi) – Trans Harbour Line
 *   10  → Slow DOWN (towards Panvel / Vashi) – Trans Harbour Line
 *   10A → Slow DOWN (towards Panvel / Vashi) – Trans Harbour Line
 *         All Trans Harbour trains start from Thane
 *
 * Graph-node mapping – the level-0 (platform-level) stairway/escalator
 * nodes that sit mid-platform, giving the pathfinder a sensible target:
 *   Platform 1   → node 104
 *   Platform 2   → node 80
 *   Platform 3   → node 81
 *   Platform 4   → node 82
 *   Platform 5   → node 83
 *   Platform 6   → node 84
 *   Platform 7   → node 85
 *   Platform 8   → node 86
 *   Platform 9   → node 87
 *   Platform 10  → node 89
 *   Platform 10A → node 89   (shares node with PF 10)
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
        // UP direction (Central Line towards CSMT)
        "CSMT", "Masjid", "Sandhurst Road", "Byculla", "Chinchpokli", "Currey Road",
        "Parel", "Dadar", "Matunga", "Sion", "Kurla", "Vidyavihar", "Ghatkopar",
        "Vikhroli", "Kanjurmarg", "Bhandup", "Nahur", "Mulund",
        // DOWN direction – Main Line
        "Kalwa", "Mumbra", "Diva", "Kopar", "Dombivli", "Thakurli", "Kalyan",
        "Shahad", "Ambivli", "Titwala", "Khadavali", "Vasind", "Asangaon",
        "Atgaon", "Khardi", "Kasara",
        // DOWN direction – Karjat branch (from Kalyan)
        "Vitthalwadi", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani",
        "Neral", "Bhivpuri Road", "Karjat",
        // Trans Harbour Line
        "Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe",
        "Sanpada", "Juinagar", "Nerul", "Seawoods-Darave", "CBD Belapur",
        "Kharghar", "Mansarovar", "Khandeshwar", "Panvel",
        "Vashi"
    ).sorted()

    // ── schedule ──────────────────────────────────────────────────────────

    val schedule: List<TrainSchedule> = listOf(

        // ═══════════════════════════════════════════════════════════════════
        //  SLOW DOWN – Platform 2 (Slow / Semi-Fast DOWN)
        //  towards Kasara / Karjat
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("98101", TrainType.SLOW,      TrainDirection.DOWN, 2, t(5,58),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind", "Asangaon")),
        TrainSchedule("98107", TrainType.SLOW,      TrainDirection.DOWN, 2, t(6,25),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath")),
        TrainSchedule("98111", TrainType.SLOW,      TrainDirection.DOWN, 2, t(6,52),  "Titwala",
            listOf("Dombivli", "Kalyan", "Shahad", "Ambivli")),
        TrainSchedule("98117", TrainType.SLOW,      TrainDirection.DOWN, 2, t(7,28),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Khadavali", "Vasind")),
        TrainSchedule("98123", TrainType.SLOW,      TrainDirection.DOWN, 2, t(8,8),   "Karjat",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98129", TrainType.SLOW,      TrainDirection.DOWN, 2, t(8,50),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98135", TrainType.SLOW,      TrainDirection.DOWN, 2, t(9,35),  "Titwala",
            listOf("Dombivli", "Kalyan", "Shahad", "Ambivli")),
        TrainSchedule("98141", TrainType.SLOW,      TrainDirection.DOWN, 2, t(11,10), "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98147", TrainType.SLOW,      TrainDirection.DOWN, 2, t(14,45), "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur")),
        TrainSchedule("98155", TrainType.SLOW,      TrainDirection.DOWN, 2, t(18,15), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),
        TrainSchedule("98161", TrainType.SLOW,      TrainDirection.DOWN, 2, t(20,30), "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98165", TrainType.SLOW,      TrainDirection.DOWN, 2, t(22,0),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),
        // Semi-Fast on PF 2
        TrainSchedule("98121", TrainType.SEMI_FAST, TrainDirection.DOWN, 2, t(7,55),  "Kasara",
            listOf("Kalyan", "Titwala", "Vasind")),

        // ═══════════════════════════════════════════════════════════════════
        //  SLOW DOWN – Platform 3 (Thane-start trains)
        //  towards Kasara / Karjat
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("98201", TrainType.SLOW,      TrainDirection.DOWN, 3, t(6,18),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98203", TrainType.SLOW,      TrainDirection.DOWN, 3, t(7,5),   "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98205", TrainType.SLOW,      TrainDirection.DOWN, 3, t(7,42),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ulhasnagar", "Ambernath")),
        TrainSchedule("98207", TrainType.SLOW,      TrainDirection.DOWN, 3, t(8,35),  "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind", "Asangaon")),
        TrainSchedule("98209", TrainType.SLOW,      TrainDirection.DOWN, 3, t(9,20),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98211", TrainType.SLOW,      TrainDirection.DOWN, 3, t(10,25), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala", "Vasind")),
        TrainSchedule("98213", TrainType.SLOW,      TrainDirection.DOWN, 3, t(13,20), "Kasara",
            listOf("Dombivli", "Kalyan", "Titwala")),
        TrainSchedule("98215", TrainType.SLOW,      TrainDirection.DOWN, 3, t(17,5),  "Badlapur",
            listOf("Dombivli", "Kalyan", "Ambernath")),
        TrainSchedule("98217", TrainType.SLOW,      TrainDirection.DOWN, 3, t(19,0),  "Karjat",
            listOf("Dombivli", "Kalyan", "Ambernath", "Badlapur", "Vangani", "Neral")),
        TrainSchedule("98219", TrainType.SLOW,      TrainDirection.DOWN, 3, t(21,15), "Kalyan",
            listOf("Dombivli")),

        // ═══════════════════════════════════════════════════════════════════
        //  FAST / SEMI-FAST DOWN – Platform 5
        //  towards Kalyan / Kasara / Karjat
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("98103", TrainType.FAST,      TrainDirection.DOWN, 5, t(6,10),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98109", TrainType.FAST,      TrainDirection.DOWN, 5, t(6,40),  "Kasara",
            listOf("Kalyan", "Titwala", "Vasind")),
        TrainSchedule("98115", TrainType.FAST,      TrainDirection.DOWN, 5, t(7,15),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98125", TrainType.FAST,      TrainDirection.DOWN, 5, t(8,22),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98131", TrainType.SEMI_FAST, TrainDirection.DOWN, 5, t(9,5),   "Kasara",
            listOf("Kalyan", "Titwala")),
        TrainSchedule("98137", TrainType.FAST,      TrainDirection.DOWN, 5, t(10,0),  "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98143", TrainType.FAST,      TrainDirection.DOWN, 5, t(12,15), "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98149", TrainType.FAST,      TrainDirection.DOWN, 5, t(16,10), "Kasara",
            listOf("Kalyan", "Titwala")),
        TrainSchedule("98153", TrainType.FAST,      TrainDirection.DOWN, 5, t(17,30), "Kalyan",
            listOf("Dombivli")),
        TrainSchedule("98159", TrainType.FAST,      TrainDirection.DOWN, 5, t(19,45), "Kasara",
            listOf("Kalyan")),

        // ═══════════════════════════════════════════════════════════════════
        //  SLOW UP – Platform 1 (Thane-start trains)
        //  towards CSMT
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("97201", TrainType.SLOW,      TrainDirection.UP, 1, t(5,45),  "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97203", TrainType.SLOW,      TrainDirection.UP, 1, t(6,30),  "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97205", TrainType.SLOW,      TrainDirection.UP, 1, t(7,15),  "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97207", TrainType.SLOW,      TrainDirection.UP, 1, t(8,0),   "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97209", TrainType.SLOW,      TrainDirection.UP, 1, t(8,45),  "Dadar",
            listOf("Mulund", "Ghatkopar", "Kurla", "Sion")),
        TrainSchedule("97211", TrainType.SLOW,      TrainDirection.UP, 1, t(9,30),  "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97213", TrainType.SLOW,      TrainDirection.UP, 1, t(11,0),  "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97215", TrainType.SLOW,      TrainDirection.UP, 1, t(14,0),  "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97217", TrainType.SLOW,      TrainDirection.UP, 1, t(17,0),  "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97219", TrainType.SLOW,      TrainDirection.UP, 1, t(20,0),  "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Dadar")),

        // ═══════════════════════════════════════════════════════════════════
        //  SLOW / SEMI-FAST UP – Platform 4
        //  towards CSMT (through trains from Kalyan/Kasara/Karjat)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("97102", TrainType.SLOW,      TrainDirection.UP, 4, t(6,5),   "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97106", TrainType.SLOW,      TrainDirection.UP, 4, t(6,45),  "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97110", TrainType.SLOW,      TrainDirection.UP, 4, t(7,30),  "Dadar",
            listOf("Mulund", "Nahur", "Ghatkopar", "Kurla", "Sion")),
        TrainSchedule("97114", TrainType.SLOW,      TrainDirection.UP, 4, t(8,15),  "CSMT",
            listOf("Mulund", "Nahur", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97118", TrainType.SLOW,      TrainDirection.UP, 4, t(9,10),  "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97122", TrainType.SLOW,      TrainDirection.UP, 4, t(10,20), "Dadar",
            listOf("Mulund", "Ghatkopar", "Kurla")),
        TrainSchedule("97126", TrainType.SLOW,      TrainDirection.UP, 4, t(12,10), "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97130", TrainType.SLOW,      TrainDirection.UP, 4, t(14,50), "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97134", TrainType.SLOW,      TrainDirection.UP, 4, t(17,40), "CSMT",
            listOf("Mulund", "Bhandup", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97138", TrainType.SLOW,      TrainDirection.UP, 4, t(19,30), "CSMT",
            listOf("Mulund", "Bhandup", "Vikhroli", "Ghatkopar", "Kurla", "Sion", "Dadar")),
        TrainSchedule("97142", TrainType.SLOW,      TrainDirection.UP, 4, t(21,0),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97144", TrainType.SLOW,      TrainDirection.UP, 4, t(22,10), "Dadar",
            listOf("Mulund", "Ghatkopar", "Kurla")),
        // Semi-Fast on PF 4
        TrainSchedule("97116", TrainType.SEMI_FAST, TrainDirection.UP, 4, t(8,40),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),

        // ═══════════════════════════════════════════════════════════════════
        //  FAST / SEMI-FAST UP – Platform 6
        //  towards CSMT (through fast trains from Kalyan/Kasara)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("97104", TrainType.FAST,      TrainDirection.UP, 6, t(6,20),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97108", TrainType.FAST,      TrainDirection.UP, 6, t(7,10),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97112", TrainType.FAST,      TrainDirection.UP, 6, t(7,55),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97120", TrainType.FAST,      TrainDirection.UP, 6, t(9,45),  "CSMT",
            listOf("Mulund", "Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97124", TrainType.FAST,      TrainDirection.UP, 6, t(11,0),  "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97128", TrainType.FAST,      TrainDirection.UP, 6, t(13,30), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97132", TrainType.FAST,      TrainDirection.UP, 6, t(16,20), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97136", TrainType.FAST,      TrainDirection.UP, 6, t(18,50), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),
        TrainSchedule("97140", TrainType.FAST,      TrainDirection.UP, 6, t(20,15), "CSMT",
            listOf("Ghatkopar", "Kurla", "Dadar")),

        // ═══════════════════════════════════════════════════════════════════
        //  OUTSTATION – Platform 7 (Fast UP, express/long-route trains)
        //  towards CSMT
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("12124", TrainType.OUTSTATION, TrainDirection.UP, 7, t(6,45),  "CSMT",
            listOf("Dadar")),
        TrainSchedule("12126", TrainType.OUTSTATION, TrainDirection.UP, 7, t(8,30),  "CSMT",
            listOf("Dadar")),
        TrainSchedule("12128", TrainType.OUTSTATION, TrainDirection.UP, 7, t(10,15), "CSMT",
            listOf("Dadar")),
        TrainSchedule("12130", TrainType.OUTSTATION, TrainDirection.UP, 7, t(14,0),  "CSMT",
            listOf("Dadar")),
        TrainSchedule("12132", TrainType.OUTSTATION, TrainDirection.UP, 7, t(17,30), "CSMT",
            listOf("Dadar")),
        TrainSchedule("12134", TrainType.OUTSTATION, TrainDirection.UP, 7, t(20,0),  "CSMT",
            listOf("Dadar")),

        // ═══════════════════════════════════════════════════════════════════
        //  OUTSTATION – Platform 8 (towards CSMT)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("11024", TrainType.OUTSTATION, TrainDirection.UP, 8, t(7,15),  "CSMT",
            listOf("Dadar")),
        TrainSchedule("11026", TrainType.OUTSTATION, TrainDirection.UP, 8, t(11,30), "CSMT",
            listOf("Dadar")),
        TrainSchedule("11028", TrainType.OUTSTATION, TrainDirection.UP, 8, t(16,0),  "CSMT",
            listOf("Dadar")),
        TrainSchedule("11030", TrainType.OUTSTATION, TrainDirection.UP, 8, t(19,45), "CSMT",
            listOf("Dadar")),

        // ═══════════════════════════════════════════════════════════════════
        //  TRANS HARBOUR LINE – Platforms 9, 10 (Thane-start, towards Panvel/Vashi)
        // ═══════════════════════════════════════════════════════════════════

        TrainSchedule("99301", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(5,30),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99303", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(6,0),   "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99305", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(6,25),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99307", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(6,50),  "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99309", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(7,10),  "Panvel",
            listOf("Airoli", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99311", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(7,35),  "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99313", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(7,55),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99315", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(8,20),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99317", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(8,45),  "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99319", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(9,15),  "Panvel",
            listOf("Airoli", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99321", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(10,0),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Sanpada", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99323", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(11,30), "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99325", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(13,0),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99327", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(15,0),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99329", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(16,30), "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99331", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(17,15), "Panvel",
            listOf("Airoli", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99333", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(18,0),  "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99335", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(18,45), "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99337", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(19,30), "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Sanpada", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99339", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(20,30), "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Juinagar", "Nerul", "CBD Belapur", "Kharghar")),
        TrainSchedule("99341", TrainType.SLOW,      TrainDirection.HARBOUR,  9, t(21,30), "Vashi",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada")),
        TrainSchedule("99343", TrainType.SLOW,      TrainDirection.HARBOUR, 10, t(22,30), "Panvel",
            listOf("Airoli", "Rabale", "Ghansoli", "Kopar Khairane", "Turbhe", "Sanpada", "Nerul", "CBD Belapur", "Kharghar"))
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