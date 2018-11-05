package com.pluscubed.velociraptor.api

import java.util.*

data class LimitResponse(
    val fromCache: Boolean = false,
    val debugInfo: String = "",
    val origin: Int = ORIGIN_INVALID,
    val error: Throwable? = null,
    /**
     * In km/h, -1 if limit does not exist
     */
    val speedLimit: Int = -1,
    val roadName: String = "",
    val coords: List<Coord> = ArrayList(),
    val timestamp: Long = 0
) {

    val isEmpty: Boolean
        get() = coords.isEmpty()

    fun initDebugInfo(): LimitResponse {
        val origin = getLimitProviderString(origin)

        var text = "\nOrigin: " + origin +
                "\n--From cache: " + fromCache
        if (error == null) {
            text += "\n--Road name: " + roadName +
                    "\n--Coords: " + coords +
                    "\n--Limit: " + speedLimit
        } else {
            text += "\n--Error: " + error.toString()
        }

        return copy(debugInfo = debugInfo + text)
    }

    companion object {
        const val ORIGIN_INVALID = -1
        const val ORIGIN_HERE = 2
        const val ORIGIN_TOMTOM = 1
        const val ORIGIN_OSM = 0

        internal fun getLimitProviderString(origin: Int): String {
            var provider = ""
            when (origin) {
                LimitResponse.ORIGIN_HERE -> provider = "HERE"
                LimitResponse.ORIGIN_TOMTOM -> provider = "TomTom"
                LimitResponse.ORIGIN_OSM -> provider = "OSM"
                -1 -> provider = "?"
                else -> provider = origin.toString()
            }
            return provider
        }
    }
}
