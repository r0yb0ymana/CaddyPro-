package com.caddypro.app.domain.model

import java.util.UUID

/**
 * Club domain model
 *
 * Represents a golf club with distances and miss bias for strategy calculations.
 *
 * AC12: Carry distance must be less than or equal to total distance
 * AC13: Club type determines sort order within the bag
 */
data class Club(
    val id: String = UUID.randomUUID().toString(),
    val bagId: String,
    val name: String,
    val type: ClubType,
    val loft: Float? = null,
    val carryDistance: Int,
    val totalDistance: Int,
    val missBias: MissBias = MissBias.STRAIGHT,
    val sortOrder: Int = type.sortOrder,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    /**
     * Validate club data
     *
     * AC12: Carry distance must be <= total distance
     * Putter is exempt from distance > 0 requirement
     */
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (type == ClubType.PUTTER) return carryDistance >= 0 && totalDistance >= 0
        return carryDistance > 0 &&
                totalDistance > 0 &&
                carryDistance <= totalDistance
    }

    /**
     * Validate carry <= total distance
     * Putter is exempt from distance > 0 requirement
     */
    fun validateDistances(): String? {
        if (type == ClubType.PUTTER) {
            return when {
                carryDistance < 0 -> "Carry distance cannot be negative"
                totalDistance < 0 -> "Total distance cannot be negative"
                else -> null
            }
        }
        return when {
            carryDistance <= 0 -> "Carry distance must be greater than 0"
            totalDistance <= 0 -> "Total distance must be greater than 0"
            carryDistance > totalDistance -> "Carry distance cannot exceed total distance"
            else -> null
        }
    }

    companion object {
        /**
         * Quick Add: Standard 14-club set with typical distances
         *
         * AC11: Quick Add populates a standard 14-club set with typical distances
         * Distances are in yards for typical male golfer (will be converted based on user units)
         */
        fun standardSet(bagId: String): List<Club> {
            return listOf(
                // Driver
                Club(
                    bagId = bagId,
                    name = "Driver",
                    type = ClubType.DRIVER,
                    loft = 10.5f,
                    carryDistance = 240,
                    totalDistance = 260,
                    missBias = MissBias.STRAIGHT
                ),
                // Woods
                Club(
                    bagId = bagId,
                    name = "3 Wood",
                    type = ClubType.WOOD,
                    loft = 15f,
                    carryDistance = 220,
                    totalDistance = 235,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "5 Wood",
                    type = ClubType.WOOD,
                    loft = 18f,
                    carryDistance = 200,
                    totalDistance = 215,
                    missBias = MissBias.STRAIGHT
                ),
                // Hybrids
                Club(
                    bagId = bagId,
                    name = "3 Hybrid",
                    type = ClubType.HYBRID,
                    loft = 21f,
                    carryDistance = 185,
                    totalDistance = 195,
                    missBias = MissBias.STRAIGHT
                ),
                // Irons (4-9)
                Club(
                    bagId = bagId,
                    name = "4 Iron",
                    type = ClubType.IRON,
                    loft = 24f,
                    carryDistance = 175,
                    totalDistance = 185,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "5 Iron",
                    type = ClubType.IRON,
                    loft = 27f,
                    carryDistance = 165,
                    totalDistance = 175,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "6 Iron",
                    type = ClubType.IRON,
                    loft = 30f,
                    carryDistance = 155,
                    totalDistance = 165,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "7 Iron",
                    type = ClubType.IRON,
                    loft = 34f,
                    carryDistance = 145,
                    totalDistance = 155,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "8 Iron",
                    type = ClubType.IRON,
                    loft = 38f,
                    carryDistance = 135,
                    totalDistance = 145,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "9 Iron",
                    type = ClubType.IRON,
                    loft = 42f,
                    carryDistance = 125,
                    totalDistance = 135,
                    missBias = MissBias.STRAIGHT
                ),
                // Wedges
                Club(
                    bagId = bagId,
                    name = "PW",
                    type = ClubType.WEDGE,
                    loft = 46f,
                    carryDistance = 115,
                    totalDistance = 120,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "SW",
                    type = ClubType.WEDGE,
                    loft = 54f,
                    carryDistance = 90,
                    totalDistance = 95,
                    missBias = MissBias.STRAIGHT
                ),
                Club(
                    bagId = bagId,
                    name = "LW",
                    type = ClubType.WEDGE,
                    loft = 60f,
                    carryDistance = 70,
                    totalDistance = 75,
                    missBias = MissBias.STRAIGHT
                ),
                // Putter
                Club(
                    bagId = bagId,
                    name = "Putter",
                    type = ClubType.PUTTER,
                    loft = 3f,
                    carryDistance = 0,
                    totalDistance = 0,
                    missBias = MissBias.STRAIGHT
                )
            )
        }
    }
}
