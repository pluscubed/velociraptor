package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.pluscubed.velociraptor.api.Coord;
import com.pluscubed.velociraptor.api.LimitProvider;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.room.Room;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class LimitCache implements LimitProvider {

    private static LimitCache instance;

    private final AppDatabase db;
    private final Scheduler scheduler;

    LimitCache(Context context, Scheduler scheduler) {
        db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "cache.db")
                .fallbackToDestructiveMigration()
                .build();
        this.scheduler = scheduler;
    }


    public static LimitCache getInstance(Context context) {
        if (instance == null) {
            instance = new LimitCache(context, Schedulers.io());
        }
        return instance;
    }

    private static boolean isLocationOnPath(Coord p1, Coord p2, Coord t) {
        List<LatLng> latLngs = Arrays.asList(p1.toLatLng(), p2.toLatLng());

        return PolyUtil.isLocationOnPath(t.toLatLng(), latLngs, false, 15);
    }

    public void put(LimitResponse response) {
        if (response.coords().isEmpty()) {
            return;
        }

        List<Way> ways = Way.Companion.fromResponse(response);
        List<Long> ids = db.wayDao().put(ways);
        for (int i = 0; i < ways.size(); i++) {
            Way way = ways.get(i);
            Timber.d("Cache put: " + ids.get(i) + " - " + way.toString());
        }
    }

    @Override
    public Observable<LimitResponse> getSpeedLimit(Location location, LimitResponse lastResponse) {
        String lastRoadName = lastResponse == null ? null : lastResponse.roadName();
        return get(lastRoadName, new Coord(location));
    }

    /**
     * Returns all responses matching with the coordinate, ordered by similarity to previous road name
     * Or if nothing matches, return an empty response
     */
    public Observable<LimitResponse> get(final String previousRoadName, final Coord coord) {
        return Observable.defer(() -> {
            LimitCache.this.cleanup();

            List<Way> selectedWays = db.wayDao()
                    .selectByCoord(coord.lat, Math.pow(Math.cos(Math.toRadians(coord.lat)), 2), coord.lon);

            return Observable.from(selectedWays)
                    .filter(way -> {
                        Coord coord1 = new Coord(way.getLat1(), way.getLon1());
                        Coord coord2 = new Coord(way.getLat2(), way.getLon2());
                        return isLocationOnPath(coord1, coord2, coord);
                    })
                    .toList()
                    .flatMap(ways -> {
                        if (ways.isEmpty()) {
                            return Observable.just(
                                    LimitResponse.builder()
                                            .setTimestamp(System.currentTimeMillis())
                                            .setFromCache(true)
                                            .initDebugInfo()
                                            .build()
                            );
                        }

                        Collections.sort(ways, (way1, way2) -> {
                            //Higher origin = further to the front
                            //Higher road similarity = further to the front
                            int heuristic2 = way2.getOrigin() + getRoadNameSimilarity(way2, previousRoadName);
                            int heuristic1 = way1.getOrigin() + getRoadNameSimilarity(way1, previousRoadName);
                            return Integer.compare(heuristic2, heuristic1);
                        });

                        return Observable.from(ways)
                                .map(limitCacheWay -> limitCacheWay.toResponse()
                                        .setFromCache(true)
                                        .initDebugInfo()
                                        .build());
                    })
                    .onErrorReturn(throwable -> {
                        return LimitResponse.builder()
                                .setTimestamp(System.currentTimeMillis())
                                .setFromCache(true)
                                .setError(throwable)
                                .initDebugInfo()
                                .build();
                    });
        }).subscribeOn(scheduler);

    }

    private int getRoadNameSimilarity(Way way, String previousRoadName) {
        if (way.getRoad() == null) {
            return 0;
        }

        return Utils.levenshteinDistance(way.getRoad(), previousRoadName);
    }


    private void cleanup() {
        db.wayDao().cleanup(System.currentTimeMillis());
    }
}
