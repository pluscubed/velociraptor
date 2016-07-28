package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.location.Location;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pluscubed.velociraptor.api.ApiResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;

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
    List<ApiResponse> responses;

    private SpeedLimitCache(File cache) {
        objectMapper = new ObjectMapper();
        cacheFile = cache;

        try {
            cacheFile.createNewFile();
            responses = objectMapper.readValue(cacheFile, new TypeReference<List<ApiResponse>>() {
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

    private static double crossTrackDist(Coord p1, Coord p2, Coord t) {
        Location a = p1.toLocation();
        Location b = p2.toLocation();
        Location x = t.toLocation();

        return Math.abs(Math.asin(Math.sin(a.distanceTo(x) / 6371008) * Math.sin(Math.toRadians(a.bearingTo(x) - a.bearingTo(b)))) * 6371008);
    }

    private static boolean isOnSegment(Coord p1, Coord p2, Coord t) {
        Location a = p1.toLocation();
        Location b = p2.toLocation();
        Location x = t.toLocation();

        double ab = a.distanceTo(b);
        return Math.abs(ab - a.distanceTo(x) - x.distanceTo(b)) < 8;
    }

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

        if (responses.contains(response)) {
            return;
        }

        responses.add(response);
        Collections.sort(responses, new CentroidLatitudeComparator());

        try {
            cleanup();
            cacheFile.createNewFile();
            objectMapper.writeValue(cacheFile, responses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void remove(ApiResponse response) {
        responses.remove(response);
        try {
            cacheFile.createNewFile();
            objectMapper.writeValue(cacheFile, responses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Observable<ApiResponse> get(final String[] previousNames, final Coord coord) {
        return Observable.defer(new Func0<Observable<ApiResponse>>() {
            @Override
            public Observable<ApiResponse> call() {
                cleanup();

                ApiResponse fluffedCoord = new ApiResponse();
                fluffedCoord.coords = new ArrayList<>();
                fluffedCoord.coords.add(coord);
                int latIndex = Collections.binarySearch(responses, fluffedCoord, new CentroidLatitudeComparator());

                if (latIndex < 0) {
                    latIndex = -(latIndex + 1);
                }

                TreeMap<Double, ApiResponse> closeResponses = new TreeMap<>();
                int max = Math.min(latIndex + 5, responses.size());
                for (int i = latIndex; i < max; i++) {
                    addClosePaths(coord, closeResponses, i);
                }
                int min = Math.max(latIndex - 6, 0);
                for (int i = latIndex - 1; i >= min; i--) {
                    addClosePaths(coord, closeResponses, i);
                }

                ApiResponse finalResponse = null;
                for (ApiResponse response : closeResponses.values()) {
                    if (response.roadNames != null && previousNames != null &&
                            (response.roadNames[0] != null && response.roadNames[0].equals(previousNames[0]) ||
                                    response.roadNames[1] != null && response.roadNames[1].equals(previousNames[1]))) {
                        finalResponse = response;
                        break;
                    }

                    //Closest path
                    if (finalResponse == null) {
                        finalResponse = response;
                    }
                }

                if (finalResponse == null) {
                    return Observable.empty();
                }

                finalResponse.fromCache = true;
                return Observable.just(finalResponse);
            }
        });

    }

    private void addClosePaths(Coord coord, Map<Double, ApiResponse> closeResponses, int responsesIndex) {
        ApiResponse apiResponse = responses.get(responsesIndex);
        for (int j = 0; j < apiResponse.coords.size() - 1; j++) {
            Coord coord1 = apiResponse.coords.get(j);
            Coord coord2 = apiResponse.coords.get(j + 1);

            if (isOnSegment(coord1, coord2, coord)) {
                closeResponses.put(crossTrackDist(coord1, coord2, coord), apiResponse);
                break;
            }
        }
    }

    private void cleanup() {
        List<ApiResponse> responsesCopy = new ArrayList<>(responses);
        Collections.sort(responsesCopy, new ResponseTimestampComparator());

        for (Iterator<ApiResponse> iterator = responsesCopy.iterator(); iterator.hasNext(); ) {
            ApiResponse response = iterator.next();
            //Make sure cache is less than 1 week old
            if (System.currentTimeMillis() - response.timestamp > VALID_CACHE_MS) {
                iterator.remove();
                remove(response);
            } else {
                break;
            }
        }
    }

    private class CentroidLatitudeComparator implements Comparator<ApiResponse> {

        @Override
        public int compare(ApiResponse response1, ApiResponse response2) {
            Coord centroid1 = getCentroid(response1);
            Coord centroid2 = getCentroid(response2);

            return Double.compare(centroid1.lat, centroid2.lat);
        }
    }

    private class ResponseTimestampComparator implements Comparator<ApiResponse> {

        @Override
        public int compare(ApiResponse response1, ApiResponse response2) {
            return Long.valueOf(response1.timestamp).compareTo(response2.timestamp);
        }
    }
}
