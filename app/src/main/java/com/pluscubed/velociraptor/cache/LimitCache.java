package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.pluscubed.velociraptor.api.Coord;
import com.pluscubed.velociraptor.api.LimitResponse;
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

public class LimitCache {

    private static LimitCache instance;

    private final BriteDatabase db;
    private final LimitCacheWay.Put put;
    private final LimitCacheWay.Cleanup cleanup;
    private final LimitCacheWay.Update_way update;

    LimitCache(Context context, Scheduler scheduler) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        LimitCacheSqlHelper helper = new LimitCacheSqlHelper(context);
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

    /**
     * Returns the road segment matching most closely with the coord & previous road name.
     * Returns empty if the coord is not on any segments in the database.
     */
    public Observable<LimitResponse> get(final String previousName, final Coord coord) {
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
                            return Observable.empty();
                        }

                        Collections.sort(ways, (way1, way2) -> Integer.compare((int) way2.origin(), (int) way1.origin()));

                        List<LimitCacheWay> validWays = Observable.from(ways)
                                .filter(way -> way.maxspeed() != 0)
                                .toList()
                                .toBlocking().first();

                        LimitResponse.Builder response;
                        if (!validWays.isEmpty()) {
                            response = validWays.get(0).toResponse();
                            for (LimitCacheWay way : validWays) {
                                if (way.road() != null && way.road().equals(previousName)) {
                                    response = way.toResponse();
                                    break;
                                }
                            }
                        } else {
                            response = ways.get(0).toResponse();
                        }

                        response.setFromCache(true);
                        return Observable.just(response.build());
                    });
        });

    }


    private void cleanup() {
        cleanup.bind(System.currentTimeMillis());
        db.executeUpdateDelete(cleanup.table, cleanup.program);
    }
}
