package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.pluscubed.velociraptor.api.ApiResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SpeedLimitCache {

    private static SpeedLimitCache instance;

    private final BriteDatabase db;
    private final wayModel.Put put;
    private final wayModel.Cleanup cleanup;
    private final wayModel.Update_way update;

    SpeedLimitCache(Context context, Scheduler scheduler) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        CacheOpenHelper helper = new CacheOpenHelper(context);
        db = sqlBrite.wrapDatabaseHelper(helper, scheduler);
        SQLiteDatabase writableDatabase = db.getWritableDatabase();

        put = new Way.Put(writableDatabase);
        update = new Way.Update_way(writableDatabase);
        cleanup = new Way.Cleanup(writableDatabase);
    }


    public static SpeedLimitCache getInstance(Context context) {
        if (instance == null) {
            instance = new SpeedLimitCache(context, Schedulers.io());
        }
        return instance;
    }

    private static double crossTrackDist(Coord p1, Coord p2, Coord t) {
        Location a = p1.toLocation();
        Location b = p2.toLocation();
        Location x = t.toLocation();

        return Math.abs(Math.asin(Math.sin(a.distanceTo(x) / 6371008) * Math.sin(Math.toRadians(a.bearingTo(x) - a.bearingTo(b)))) * 6371008);
    }

    /*private static boolean isOnSegment(Coord p1, Coord p2, Coord t) {
        Location a = p1.toLocation();
        Location b = p2.toLocation();
        Location x = t.toLocation();

        double ab = a.distanceTo(b);
        //If difference between straight line and going through point is than 8 meters
        return Math.abs(ab - a.distanceTo(x) - x.distanceTo(b)) < 8;
    }*/

    private static Coord getCentroid(ApiResponse response) {
        Coord centroid = new Coord();
        for (Coord coord : response.coords) {
            centroid.lat += coord.lat;
            centroid.lon += coord.lon;
        }
        centroid.lat /= response.coords.size();

        return centroid;
    }

    public void put(ApiResponse response) {
        if (response.coords == null) {
            return;
        }

        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            List<Way> ways = Way.fromResponse(response);
            for (Way way : ways) {
                update.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road());
                int rowsAffected = db.executeUpdateDelete(update.table, update.program);

                if (rowsAffected != 1) {
                    put.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road());
                    long rowId = db.executeInsert(put.table, put.program);
                    Timber.d("Cache put: " + rowId + " - " + way.toString());
                }
            }

            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public Observable<ApiResponse> get(final String previousName, final Coord coord) {
        return Observable.defer(new Func0<Observable<ApiResponse>>() {
            @Override
            public Observable<ApiResponse> call() {
                SpeedLimitCache.this.cleanup();

                SqlDelightStatement selectStatement = Way.FACTORY.select_by_coord(coord.lat, Math.pow(Math.cos(Math.toRadians(coord.lat)), 2), coord.lon);

                return db.createQuery(selectStatement.tables, selectStatement.statement, selectStatement.args)
                        .mapToList(Way.SELECT_BY_COORD::map)
                        .take(1)
                        .flatMap(Observable::from)
                        .filter(way -> {
                            Coord coord1 = new Coord(way.lat1(), way.lon1());
                            Coord coord2 = new Coord(way.lat2(), way.lon2());
                            double crossTrackDist = crossTrackDist(coord1, coord2, coord);
                            return crossTrackDist < 15 /*&& isOnSegment(coord1, coord2, coord)*/;
                        })
                        .toList()
                        .flatMap(new Func1<List<Way>, Observable<ApiResponse>>() {
                            @Override
                            public Observable<ApiResponse> call(List<Way> ways) {
                                if (ways.isEmpty()) {
                                    return Observable.empty();
                                }

                                ApiResponse response = ways.get(0).toResponse();
                                for (Way way : ways) {
                                    if (way.road() != null && way.road().equals(previousName)) {
                                        response = way.toResponse();
                                        break;
                                    }
                                }

                                response.fromCache = true;
                                return Observable.just(response);
                            }
                        });
            }
        });

    }


    private void cleanup() {
        cleanup.bind(System.currentTimeMillis());
        db.executeUpdateDelete(cleanup.table, cleanup.program);
    }
}
