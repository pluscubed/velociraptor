package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.pluscubed.velociraptor.api.Coord;
import com.pluscubed.velociraptor.api.LimitProvider;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.utils.Utils;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class LimitCache implements LimitProvider {

    private static LimitCache instance;

    private final BriteDatabase db;
    private final LimitCacheWay.Put put;
    private final LimitCacheWay.Cleanup cleanup;
    private final LimitCacheWay.Update_way update;
    private final Context context;

    LimitCache(Context context, Scheduler scheduler) {
        this.context = context;

        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        LimitCacheSqlHelper helper = new LimitCacheSqlHelper(this.context);
        db = sqlBrite.wrapDatabaseHelper(helper, scheduler);
        SQLiteDatabase writableDatabase = db.getWritableDatabase();

        put = new LimitCacheWay.Put(writableDatabase);
        update = new LimitCacheWay.Update_way(writableDatabase);
        cleanup = new LimitCacheWay.Cleanup(writableDatabase);
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

        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            List<LimitCacheWay> ways = LimitCacheWay.fromResponse(response);
            for (LimitCacheWay way : ways) {
                update.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road(), way.origin());
                int rowsAffected = db.executeUpdateDelete(update.table, update.program);

                if (rowsAffected != 1) {
                    put.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road(), way.origin());
                    long rowId = db.executeInsert(put.table, put.program);
                    Timber.d("Cache put: " + rowId + " - " + way.toString());
                }
            }

            transaction.markSuccessful();
        } finally {
            transaction.end();
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

            SqlDelightStatement selectStatement = LimitCacheWay.FACTORY.select_by_coord(coord.lat, Math.pow(Math.cos(Math.toRadians(coord.lat)), 2), coord.lon);

            return db.createQuery(selectStatement.tables, selectStatement.statement, selectStatement.args)
                    .mapToList(LimitCacheWay.SELECT_BY_COORD::map)
                    .take(1)
                    .flatMap(Observable::from)
                    .filter(way -> {
                        Coord coord1 = new Coord(way.lat1(), way.lon1());
                        Coord coord2 = new Coord(way.lat2(), way.lon2());
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
                            int heuristic2 = (int) way2.origin() + getRoadNameSimilarity(way2, previousRoadName);
                            int heuristic1 = (int) way1.origin() + getRoadNameSimilarity(way1, previousRoadName);
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
        });

    }

    private int getRoadNameSimilarity(LimitCacheWay way, String previousRoadName) {
        if (way.road() == null) {
            return 0;
        }

        return Utils.levenshteinDistance(way.road(), previousRoadName);
    }


    private void cleanup() {
        cleanup.bind(System.currentTimeMillis());
        db.executeUpdateDelete(cleanup.table, cleanup.program);
    }
}
