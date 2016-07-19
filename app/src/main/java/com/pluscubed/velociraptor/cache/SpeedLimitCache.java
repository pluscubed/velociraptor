package com.pluscubed.velociraptor.cache;

import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pluscubed.velociraptor.SpeedLimitApi;
import com.pluscubed.velociraptor.osmapi.Coord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rx.Observable;
import rx.functions.Func0;

public class SpeedLimitCache {

    public static final double VALID_CACHE_MS = 6.048e+8;
    private static SpeedLimitCache instance;
    private final ObjectMapper objectMapper;
    private final File cacheFile;
    List<SpeedLimitApi.ApiResponse> responses;

    private SpeedLimitCache(File cache) {
        objectMapper = new ObjectMapper();
        cacheFile = cache;

        try {
            cacheFile.createNewFile();
            responses = objectMapper.readValue(cacheFile, new TypeReference<List<SpeedLimitApi.ApiResponse>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            responses = new ArrayList<>();
        }
    }

    public static SpeedLimitCache getInstance(Context context) {
        return getInstance(new File(context.getCacheDir().getPath() + "/cache.json"));
    }

    public static SpeedLimitCache getInstance(File cache) {
        if (instance == null) {
            instance = new SpeedLimitCache(cache);
        }
        return instance;
    }

    //http://stackoverflow.com/questions/20231258/minimum-distance-between-a-point-and-a-line-in-latitude-longitude
    private static double distToPathMeters(Coord a, Coord b, Coord c) {
        double lat1 = a.getLat();
        double lon1 = a.getLon();
        double lat2 = b.getLat();
        double lon2 = b.getLon();
        double lat3 = c.getLat();
        double lon3 = c.getLon();

        double y = Math.sin(lon3 - lon1) * Math.cos(lat3);
        double x = Math.cos(lat1) * Math.sin(lat3) - Math.sin(lat1) * Math.cos(lat3) * Math.cos(lat3 - lat1);
        double bearing1 = Math.toDegrees(Math.atan2(y, x));

        double y2 = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x2 = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lat2 - lat1);
        double bearing2 = Math.toDegrees(Math.atan2(y2, x2));

        double lat1Rads = Math.toRadians(lat1);
        double lat3Rads = Math.toRadians(lat3);
        double dLon = Math.toRadians(lon3 - lon1);

        double distanceAC = Math.acos(Math.sin(lat1Rads) * Math.sin(lat3Rads) + Math.cos(lat1Rads) * Math.cos(lat3Rads) * Math.cos(dLon)) * 6371000;

        return Math.abs(Math.asin(Math.sin(distanceAC / 6371000) * Math.sin(Math.toRadians(bearing1) - Math.toRadians(bearing2))) * 6371000);
    }

    private static Coord getCentroid(SpeedLimitApi.ApiResponse response1) {
        Coord centroid = new Coord();
        for (Coord coord : response1.coords) {
            centroid.setLat(centroid.getLat() + coord.getLat());
            centroid.setLon(centroid.getLon() + coord.getLon());
        }
        centroid.setLat(centroid.getLat() / response1.coords.size());

        return centroid;
    }

    public void put(SpeedLimitApi.ApiResponse response) {
        if (response.coords != null) {
            responses.add(response);
            Collections.sort(responses, new PathComparator(true));

            try {
                cacheFile.createNewFile();
                objectMapper.writeValue(cacheFile, responses);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void remove(SpeedLimitApi.ApiResponse response) {
        responses.remove(response);
        try {
            cacheFile.createNewFile();
            objectMapper.writeValue(cacheFile, responses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Observable<SpeedLimitApi.ApiResponse> get(final String[] previousNames, final Coord coord) {
        return Observable.defer(new Func0<Observable<SpeedLimitApi.ApiResponse>>() {
            @Override
            public Observable<SpeedLimitApi.ApiResponse> call() {
                SpeedLimitApi.ApiResponse fluffedCoord = new SpeedLimitApi.ApiResponse();
                fluffedCoord.coords = new ArrayList<>();
                fluffedCoord.coords.add(coord);
                int latIndex = Collections.binarySearch(responses, fluffedCoord, new PathComparator(true));

                if (latIndex < 0) {
                    latIndex = -(latIndex + 1);
                }

                Map<Double, SpeedLimitApi.ApiResponse> closeEnough = new TreeMap<>();
                int max = Math.min(latIndex + 5, responses.size());
                for (int i = latIndex; i < max; i++) {
                    addPossiblePaths(coord, closeEnough, i);
                }
                int min = Math.max(latIndex - 6, 0);
                for (int i = latIndex - 1; i >= min; i--) {
                    addPossiblePaths(coord, closeEnough, i);
                }

                SpeedLimitApi.ApiResponse finalResponse = null;
                for (Iterator<SpeedLimitApi.ApiResponse> iterator = closeEnough.values().iterator(); iterator.hasNext(); ) {
                    SpeedLimitApi.ApiResponse response = iterator.next();

                    //Make sure cache is less than 1 week old
                    if (System.currentTimeMillis() - response.timestamp > VALID_CACHE_MS) {
                        iterator.remove();
                        remove(response);
                        break;
                    }

                    if (response.roadNames != null && previousNames != null &&
                            (response.roadNames[0] != null && response.roadNames[0].equals(previousNames[0]) ||
                                    response.roadNames[1] != null && response.roadNames[1].equals(previousNames[1]))) {
                        finalResponse = response;
                        break;
                    }
                }

                if (finalResponse == null && closeEnough.values().iterator().hasNext()) {
                    finalResponse = closeEnough.values().iterator().next();
                }

                if (finalResponse == null) {
                    return Observable.empty();
                }

                finalResponse.fromCache = true;
                return Observable.just(finalResponse);
            }
        });

    }

    private void addPossiblePaths(Coord coord, Map<Double, SpeedLimitApi.ApiResponse> closeEnough, int responsesIndex) {
        SpeedLimitApi.ApiResponse apiResponse = responses.get(responsesIndex);
        for (int j = 0; j < apiResponse.coords.size() - 1; j++) {
            Coord coord1 = apiResponse.coords.get(j);
            Coord coord2 = apiResponse.coords.get(j + 1);


            double dist = distToPathMeters(coord1, coord2, coord);
            if (dist < 15) {
                closeEnough.put(dist, apiResponse);
                break;
            }
        }
    }

    private class PathComparator implements Comparator<SpeedLimitApi.ApiResponse> {

        private boolean lat;

        public PathComparator(boolean lat) {
            this.lat = lat;
        }

        @Override
        public int compare(SpeedLimitApi.ApiResponse response1, SpeedLimitApi.ApiResponse response2) {
            Coord centroid1 = getCentroid(response1);
            Coord centroid2 = getCentroid(response2);

            return lat ? centroid1.getLat().compareTo(centroid2.getLat()) :
                    centroid1.getLon().compareTo(centroid2.getLon());
        }
    }

    private class ResponseTimestampComparator implements Comparator<SpeedLimitApi.ApiResponse> {

        @Override
        public int compare(SpeedLimitApi.ApiResponse response1, SpeedLimitApi.ApiResponse response2) {
            return Long.valueOf(response1.timestamp).compareTo(response2.timestamp);
        }
    }
}
