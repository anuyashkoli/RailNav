package com.app.railnav.data

object DirectionGenerator {

    fun generate(path: List<GraphNode>): List<String> {
        if (path.size < 2) return listOf("You have arrived.")

        val instructions = mutableListOf<String>()
        // Add a starting instruction
        instructions.add("Start by heading towards ${path[1].properties.node_name ?: "the next point"}.")

        // Loop through the path, looking at sets of three nodes (previous, current, next)
        for (i in 1 until path.size - 1) {
            val current = path[i].properties
            val next = path[i + 1].properties

            // Generate an instruction when the type of path changes
            if (current.node_type != next.node_type && next.node_type != null) {
                val instruction = when (next.node_type) {
                    "STAIRWAY_TOP" -> "Go towards ${next.node_name ?: "the stairs"}."
                    "STAIRWAY_BOT" -> "Go down ${current.node_name ?: "the stairs"}."
                    "LIFT_TOP" -> "Head towards ${next.node_name ?: "the lift"}."
                    "LIFT_BOT" -> "Take ${current.node_name ?: "the lift"} down."
                    "JUNCTION" -> "Continue to the junction."
                    "ENTRY/EXIT" -> "Proceed towards the ${next.node_name ?: "Exit"}."
                    else -> "Continue towards ${next.node_name ?: "the next point"}."
                }
                instructions.add(instruction)
            }
        }

        // Add a final instruction
        instructions.add("You have reached your destination: ${path.last().properties.node_name ?: "Final Point"}.")
        return instructions
    }
}