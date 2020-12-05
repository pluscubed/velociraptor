package com.pluscubed.velociraptor.api.cache

import android.content.Context
import android.location.Location
import androidx.room.Room
import com.google.maps.android.PolyUtil
import com.pluscubed.velociraptor.api.Coord
import com.pluscubed.velociraptor.api.LimitProvider
import com.pluscubed.velociraptor.api.LimitResponse
import com.pluscubed.velociraptor.api.osm.OsmLimitProvider
import com.pluscubed.velociraptor.utils.NormalizedLevenshtein
import com.pluscubed.velociraptor.utils.PrefUtils
import timber.log.Timber
import java.util.Arrays
import kotlin.Comparator

class CacheLimitProvider(private val context: Context) : LimitProvider {

    private val db: AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cache.db")
                    .fallbackToDestructiveMigration()
                    .build()

    private val normalleven = NormalizedLevenshtein()

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
    override fun getSpeedLimit(
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
                        // Sort: road name similar, then whether speed exists, then better source

                        val roadNameSimilar1 = isRoadNameSimilar(way1, previousRoadName)
                        val roadNameSimilar2 = isRoadNameSimilar(way2, previousRoadName)

                        if (roadNameSimilar1 == roadNameSimilar2) {
                            val speedExists1 = way1.maxspeed != -1
                            val speedExists2 = way2.maxspeed != -1

                            if (speedExists1 == speedExists2) {
                                //Higher origin = further to the front
                                way1.origin.compareTo(way2.origin)
                            } else {
                                speedExists1.compareTo(speedExists2)
                            }
                        } else {
                            roadNameSimilar1.compareTo(roadNameSimilar2)
                        }
                    })
                    .reversed()
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

    private fun isRoadNameSimilar(way: Way, previousRoadName: String?): Boolean {
        return getRoadNameSimilarity(way, previousRoadName) > 0.5
    }

    private fun getRoadNameSimilarity(way: Way, previousRoadName: String?): Double {
        if (previousRoadName == null)
            return 0.0
        else {
            val roadLower = way.road.toLowerCase()
            val prevRoadNameLower = previousRoadName.toLowerCase()

            //Split ref, name
            val roadNameSplittable = roadLower.contains(OsmLimitProvider.ROADNAME_DELIM)
            val previousRoadNameSplittable =
                    prevRoadNameLower.contains(OsmLimitProvider.ROADNAME_DELIM)

            if (roadNameSplittable && !previousRoadNameSplittable) {
                val split = roadLower.split(OsmLimitProvider.ROADNAME_DELIM)
                return Math.max(
                        normalleven.similarity(split[0], prevRoadNameLower),
                        normalleven.similarity(split[1], prevRoadNameLower)
                )
            } else if (!roadNameSplittable && previousRoadNameSplittable) {
                val split = prevRoadNameLower.split(OsmLimitProvider.ROADNAME_DELIM)
                return Math.max(
                        normalleven.similarity(roadLower, split[0]),
                        normalleven.similarity(roadLower, split[1])
                )
            } else {
                return normalleven.similarity(roadLower, prevRoadNameLower)
            }
        }
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
