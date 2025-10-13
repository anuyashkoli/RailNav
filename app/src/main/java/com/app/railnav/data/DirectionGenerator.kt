package com.app.railnav.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object DirectionGenerator {

    /**
     * Calculates the initial bearing (angle from North) between two geographic points.
     */
    private fun calculateBearing(nodeA: GraphNode, nodeB: GraphNode): Double {
        val lat1 = Math.toRadians(nodeA.coordinates[1])
        val lon1 = Math.toRadians(nodeA.coordinates[0])
        val lat2 = Math.toRadians(nodeB.coordinates[1])
        val lon2 = Math.toRadians(nodeB.coordinates[0])

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        // Return bearing in degrees
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /**
     * Generates a list of human-readable navigation instructions from a path of nodes.
     */
    fun generate(path: List<GraphNode>): List<String> {
        if (path.size < 2) return listOf("You have arrived at your destination.")

        val instructions = mutableListOf<String>()
        instructions.add("Start by heading towards ${path[1].properties.node_name ?: "the next point"}.")

        // Iterate through the path to find turns and changes in path type.
        for (i in 1 until path.size - 1) {
            val prevNode = path[i - 1]
            val currentNode = path[i]
            val nextNode = path[i + 1]

            val bearingIn = calculateBearing(prevNode, currentNode)
            val bearingOut = calculateBearing(currentNode, nextNode)

            // Calculate the turn angle. A positive value is a right turn, negative is left.
            var turnAngle = bearingOut - bearingIn
            if (turnAngle > 180) turnAngle -= 360
            if (turnAngle < -180) turnAngle += 360

            val currentProps = currentNode.properties
            val nextProps = nextNode.properties

            var instruction: String? = null

            // Rule 1: Generate an instruction at significant junctions based on the turn angle.
            if (currentProps.node_type == "JUNCTION") {
                when {
                    turnAngle > 45 -> instruction = "Turn right towards ${nextProps.node_name ?: "the next point"}."
                    turnAngle < -45 -> instruction = "Turn left towards ${nextProps.node_name ?: "the next point"}."
                    // Optional: Add instructions for "slight" turns if desired.
                }
            }

            // Rule 2: Generate an instruction when the type of path changes (e.g., walking onto stairs).
            if (currentProps.node_type != nextProps.node_type && nextProps.node_type != null) {
                instruction = when (nextProps.node_type) {
                    "STAIRWAY_TOP" -> "Go towards ${nextProps.node_name ?: "the stairs"}."
                    "STAIRWAY_BOT" -> "Go down ${currentProps.node_name ?: "the stairs"}."
                    "LIFT_TOP" -> "Head towards ${nextProps.node_name ?: "the lift"}."
                    "LIFT_BOT" -> "Take ${currentProps.node_name ?: "the lift"} down."
                    "ENTRY/EXIT" -> "Proceed towards the ${nextProps.node_name ?: "Exit"}."
                    else -> null // Let the turn-based instruction handle it if one was generated.
                }
            }

            if (instruction != null) {
                // To avoid duplicate instructions, only add if it's different from the last one.
                if (instructions.lastOrNull() != instruction) {
                    instructions.add(instruction)
                }
            }
        }

        instructions.add("You will arrive at your destination: ${path.last().properties.node_name ?: "Final Point"}.")
        return instructions
    }
}