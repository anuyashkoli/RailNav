package com.app.railnav.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// NEW: Data class to bundle the text with its physical map location
data class NavigationInstruction(
    val text: String,
    val targetNode: GraphNode
)

object DirectionGenerator {

    private fun calculateBearing(nodeA: GraphNode, nodeB: GraphNode): Double {
        val lat1 = Math.toRadians(nodeA.coordinates[1])
        val lon1 = Math.toRadians(nodeA.coordinates[0])
        val lat2 = Math.toRadians(nodeB.coordinates[1])
        val lon2 = Math.toRadians(nodeB.coordinates[0])

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun generate(path: List<GraphNode>): List<NavigationInstruction> {
        if (path.size < 2) return listOf(
            NavigationInstruction("You have arrived at your destination.", path.last())
        )

        val instructions = mutableListOf<NavigationInstruction>()

        // Start instruction points to the immediate next node
        instructions.add(
            NavigationInstruction(
                "Start by heading towards ${path[1].properties.node_name ?: "the next point"}.",
                path[1]
            )
        )

        for (i in 1 until path.size - 1) {
            val prevNode = path[i - 1]
            val currentNode = path[i]
            val nextNode = path[i + 1]

            val currentProps = currentNode.properties
            val nextProps = nextNode.properties

            val bearing1 = calculateBearing(prevNode, currentNode)
            val bearing2 = calculateBearing(currentNode, nextNode)
            var angleChange = bearing2 - bearing1

            if (angleChange < -180) angleChange += 360
            if (angleChange > 180) angleChange -= 360

            var instructionText: String? = null

            // Rule 1: Angle changes (Turns)
            if (angleChange > 45 && angleChange < 135) {
                instructionText = "Turn right towards ${nextProps.node_name ?: "the next point"}."
            } else if (angleChange < -45 && angleChange > -135) {
                instructionText = "Turn left towards ${nextProps.node_name ?: "the next point"}."
            }

            // Rule 2: Path type changes (Stairs, Lifts, Exits)
            if (currentProps.node_type != nextProps.node_type && nextProps.node_type != null) {
                val typeText = when (nextProps.node_type) {
                    "STAIRWAY_TOP" -> "Go towards ${nextProps.node_name ?: "the stairs"}."
                    "STAIRWAY_BOT" -> "Go down ${currentProps.node_name ?: "the stairs"}."
                    "LIFT_TOP" -> "Head towards ${nextProps.node_name ?: "the lift"}."
                    "LIFT_BOT" -> "Take ${currentProps.node_name ?: "the lift"} down."
                    "ENTRY/EXIT" -> "Proceed towards the ${nextProps.node_name ?: "Exit"}."
                    else -> null
                }
                if (typeText != null) instructionText = typeText
            }

            if (instructionText != null) {
                val newInstruction = NavigationInstruction(instructionText, nextNode)
                if (instructions.isEmpty() || instructions.last().text != instructionText) {
                    instructions.add(newInstruction)
                }
            }
        }

        instructions.add(
            NavigationInstruction(
                "You will arrive at your destination: ${path.last().properties.node_name ?: "Final Point"}.",
                path.last()
            )
        )
        return instructions
    }
}