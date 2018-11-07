package com.pluscubed.velociraptor.api.cache

import android.content.Context
import android.location.Location
import androidx.room.Room
import com.google.maps.android.PolyUtil
import com.pluscubed.velociraptor.api.Coord
import com.pluscubed.velociraptor.api.LimitProvider
import com.pluscubed.velociraptor.api.LimitResponse
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils
import timber.log.Timber
import java.util.Arrays
import kotlin.Comparator

class CacheLimitProvider(private val context: Context) : LimitProvider {

    private val db: AppDatabase =
        Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cache.db")
            .fallbackToDestructiveMigration()
            .build()

    fun put(response: LimitResponse) {
        if (response.coords.isEmpty()) {
            return
        }

        val ways = Way.fromResponse(response)
        val ids = db.wayDao().put(ways)
        for (i in ways.indices) {
            val way = ways[i]
            Timber.d("Cache put: ${ids[i]} - $way")
        }
    }

    /**
     * Returns list with at least 1 element
     */
    override suspend fun getSpeedLimit(
        location: Location,
        lastResponse: LimitResponse?,
        origin: Int
    ): List<LimitResponse> {
        val lastRoadName = lastResponse?.roadName
        return get(lastRoadName, Coord(location))
    }

    /**
     * Returns all responses matching with the coordinate, ordered by similarity to previous road name
     * Or if nothing matches, return an empty response
     */
    fun get(previousRoadName: String?, coord: Coord): List<LimitResponse> {
        this@CacheLimitProvider.cleanup()

        val selectedWays =
            db.wayDao().selectByCoord(
                coord.lat,
                Math.pow(Math.cos(Math.toRadians(coord.lat)), 2.0),
                coord.lon
            )

        val onPathWays = selectedWays
            .filter { (_, _, _, _, lat1, lon1, lat2, lon2) ->
                val coord1 = Coord(lat1, lon1)
                val coord2 = Coord(lat2, lon2)
                isLocationOnPath(coord1, coord2, coord)
            };

        val debuggingEnabled = PrefUtils.isDebuggingEnabled(context)

        if (onPathWays.isEmpty()) {
            return listOf(
                LimitResponse(
                    timestamp = System.currentTimeMillis(),
                    fromCache = true
                ).initDebugInfo(debuggingEnabled)
            )
        }

        try {
            return onPathWays
                .sortedWith(Comparator { way1, way2 ->
                    //Higher origin = further to the front
                    //Higher road similarity = further to the front
                    val heuristic2 = way2.origin + getRoadNameSimilarity(way2, previousRoadName)
                    val heuristic1 = way1.origin + getRoadNameSimilarity(way1, previousRoadName)
                    heuristic2.compareTo(heuristic1)
                })
                .map { limitCacheWay ->
                    limitCacheWay.toResponse()
                        .copy(fromCache = true)
                        .initDebugInfo(debuggingEnabled)
                }
        } catch (e: Exception) {
            return listOf(
                LimitResponse(
                    timestamp = System.currentTimeMillis(),
                    error = e,
                    fromCache = true
                ).initDebugInfo(debuggingEnabled)
            )
        }
    }

    private fun getRoadNameSimilarity(way: Way, previousRoadName: String?): Int {
        return if (previousRoadName == null) 0
        else Utils.levenshteinDistance(way.road, previousRoadName)
    }


    private fun cleanup() {
        db.wayDao().cleanup(System.currentTimeMillis())
    }

    public fun clear() {
        db.wayDao().clear()
    }

    companion object {

        private fun isLocationOnPath(p1: Coord, p2: Coord, t: Coord): Boolean {
            val latLngs = Arrays.asList(p1.toLatLng(), p2.toLatLng())

            return PolyUtil.isLocationOnPath(t.toLatLng(), latLngs, false, 15.0)
        }
    }
}
