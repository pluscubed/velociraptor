package com.pluscubed.velociraptor.cache

import android.content.Context
import android.location.Location
import androidx.room.Room
import com.google.maps.android.PolyUtil
import com.pluscubed.velociraptor.api.Coord
import com.pluscubed.velociraptor.api.LimitProvider
import com.pluscubed.velociraptor.api.LimitResponse
import com.pluscubed.velociraptor.utils.Utils
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.Arrays
import kotlin.Comparator

class LimitCache internal constructor(context: Context, private val scheduler: Scheduler) : LimitProvider {

    private val db: AppDatabase

    init {
        db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cache.db")
                .fallbackToDestructiveMigration()
                .build()
    }

    fun put(response: LimitResponse) {
        if (response.coords().isEmpty()) {
            return
        }

        val ways = Way.fromResponse(response)
        val ids = db.wayDao().put(ways)
        for (i in ways.indices) {
            val way = ways[i]
            Timber.d("Cache put: ${ids[i]} - $way")
        }
    }

    override fun getSpeedLimit(location: Location, lastResponse: LimitResponse?): Observable<LimitResponse> {
        val lastRoadName = lastResponse?.roadName()
        return get(lastRoadName, Coord(location))
    }

    /**
     * Returns all responses matching with the coordinate, ordered by similarity to previous road name
     * Or if nothing matches, return an empty response
     */
    fun get(previousRoadName: String?, coord: Coord): Observable<LimitResponse> {
        return Observable.defer {
            this@LimitCache.cleanup()

            val selectedWays = db.wayDao()
                    .selectByCoord(coord.lat, Math.pow(Math.cos(Math.toRadians(coord.lat)), 2.0), coord.lon)

            Observable.from(selectedWays)
                    .filter { (_, _, _, _, lat1, lon1, lat2, lon2) ->
                        val coord1 = Coord(lat1, lon1)
                        val coord2 = Coord(lat2, lon2)
                        isLocationOnPath(coord1, coord2, coord)
                    }
                    .toList()
                    .flatMap { ways ->
                        if (ways.isEmpty()) {
                            return@flatMap Observable.just(
                                    LimitResponse.builder()
                                            .setTimestamp(System.currentTimeMillis())
                                            .setFromCache(true)
                                            .initDebugInfo()
                                            .build()
                            )
                        }

                        ways.sortWith(Comparator { way1, way2 ->
                            //Higher origin = further to the front
                            //Higher road similarity = further to the front
                            val heuristic2 = way2.origin + getRoadNameSimilarity(way2, previousRoadName)
                            val heuristic1 = way1.origin + getRoadNameSimilarity(way1, previousRoadName)
                            heuristic2.compareTo(heuristic1)
                        })

                        Observable.from(ways)
                                .map { limitCacheWay ->
                                    limitCacheWay.toResponse()
                                            .setFromCache(true)
                                            .initDebugInfo()
                                            .build()
                                }
                    }
                    .onErrorReturn { throwable ->
                        LimitResponse.builder()
                                .setTimestamp(System.currentTimeMillis())
                                .setFromCache(true)
                                .setError(throwable)
                                .initDebugInfo()
                                .build()
                    }
        }.subscribeOn(scheduler)

    }

    private fun getRoadNameSimilarity(way: Way, previousRoadName: String?): Int {
        return if (way.road == null) 0
        else Utils.levenshteinDistance(way.road, previousRoadName!!)
    }


    private fun cleanup() {
        db.wayDao().cleanup(System.currentTimeMillis())
    }

    companion object {
        private var instance: LimitCache? = null


        fun getInstance(context: Context): LimitCache {
            if (instance == null) {
                instance = LimitCache(context, Schedulers.io())
            }
            return instance as LimitCache
        }

        private fun isLocationOnPath(p1: Coord, p2: Coord, t: Coord): Boolean {
            val latLngs = Arrays.asList(p1.toLatLng(), p2.toLatLng())

            return PolyUtil.isLocationOnPath(t.toLatLng(), latLngs, false, 15.0)
        }
    }
}
