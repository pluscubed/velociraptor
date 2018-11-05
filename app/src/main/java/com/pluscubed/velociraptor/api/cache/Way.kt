package com.pluscubed.velociraptor.api.cache

import androidx.room.Entity
import com.pluscubed.velociraptor.api.Coord
import com.pluscubed.velociraptor.api.LimitResponse
import java.util.*

@Entity(primaryKeys = ["clat", "clon"])
data class Way(
    val clat: Double,
    val clon: Double,
    val maxspeed: Int,
    val timestamp: Long,
    val lat1: Double,
    val lon1: Double,
    val lat2: Double,
    val lon2: Double,
    val road: String,
    val origin: Int
) {

    companion object {
        fun fromResponse(response: LimitResponse): List<Way> {
            val ways = ArrayList<Way>()
            for (i in 0 until response.coords.size - 1) {
                val coord1 = response.coords[i]
                val coord2 = response.coords[i + 1]

                val clat = (coord1.lat + coord2.lat) / 2
                val clon = (coord1.lon + coord2.lon) / 2

                val way = Way(
                    clat, clon,
                    response.speedLimit,
                    response.timestamp,
                    coord1.lat, coord1.lon, coord2.lat, coord2.lon,
                    response.roadName,
                    response.origin
                )
                ways.add(way)
            }
            return ways
        }
    }

    fun toResponse(): LimitResponse {
        return LimitResponse(
            speedLimit = maxspeed,
            timestamp = timestamp,
            roadName = road,
            origin = origin,
            coords = Arrays.asList(Coord(lat1, lon1), Coord(lat2, lon2))
        )
    }

}