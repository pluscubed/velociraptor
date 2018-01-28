package com.pluscubed.velociraptor.api.osm;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.api.LimitFetcher;
import com.pluscubed.velociraptor.api.LimitInterceptor;
import com.pluscubed.velociraptor.api.LimitProvider;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.api.osm.data.Element;
import com.pluscubed.velociraptor.api.osm.data.OsmResponse;
import com.pluscubed.velociraptor.api.osm.data.Tags;
import com.pluscubed.velociraptor.cache.LimitCache;
import com.pluscubed.velociraptor.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Single;
import timber.log.Timber;

public class OsmLimitProvider implements LimitProvider {
    public static final String FB_CONFIG_OSM_APIS = "osm_apis";
    public static final String FB_CONFIG_OSM_API_ENABLED_PREFIX = "osm_api";

    public static final int OSM_RADIUS = 15;
    private Context context;
    private OkHttpClient client;
    private LimitCache cache;

    private List<OsmApiEndpoint> osmOverpassApis;

    public OsmLimitProvider(Context context, OkHttpClient client, LimitCache osmCache) {
        this.context = context;
        this.client = client;
        this.cache = osmCache;

        osmOverpassApis = new ArrayList<>();

        String endpointUrl = "https://overpass.kumi.systems/api/";
        int resId = context.getResources().getIdentifier("overpass_api", "string", context.getPackageName());
        if (resId != 0) {
            endpointUrl = context.getString(resId);
        }
        OsmApiEndpoint endpoint = new OsmApiEndpoint(endpointUrl);
        initializeOsmService(endpoint);
        osmOverpassApis.add(endpoint);

        refreshApiEndpoints();
    }

    private void initializeOsmService(OsmApiEndpoint endpoint) {
        LimitInterceptor osmInterceptor = new LimitInterceptor(new LimitInterceptor.Callback() {
            @Override
            public void updateTimeTaken(int timeTaken) {
                updateEndpointTimeTaken(timeTaken, endpoint);
            }
        });

        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(osmInterceptor)
                .build();
        Retrofit osmRest = LimitFetcher.buildRetrofit(osmClient, endpoint.getBaseUrl());

        OsmService osmService = osmRest.create(OsmService.class);
        endpoint.setService(osmService);
    }

    private void updateEndpointTimeTaken(int timeTaken, OsmApiEndpoint endpoint) {
        endpoint.setTimeTaken(timeTaken);
        Collections.sort(osmOverpassApis);
        Timber.d("Endpoints: %s", osmOverpassApis);
    }

    private void refreshApiEndpoints() {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        String apisString = remoteConfig.getString(FB_CONFIG_OSM_APIS);

        if (apisString == null || apisString.isEmpty()) {
            return;
        }

        String[] stringArray = apisString.replace("[", "").replace("]", "").split(",");

        for (int i = 0; i < stringArray.length; i++) {
            String apiHost = stringArray[i].replace("\"", "");
            boolean enabled = remoteConfig.getBoolean(FB_CONFIG_OSM_API_ENABLED_PREFIX + i);
            if (enabled) {
                OsmApiEndpoint endpoint = new OsmApiEndpoint(apiHost);
                initializeOsmService(endpoint);
                osmOverpassApis.add(endpoint);
            }
        }
    }

    private String buildQueryBody(Location location) {
        return "[out:json];" +
                "way(around:" + OSM_RADIUS + ","
                + location.getLatitude() + ","
                + location.getLongitude() +
                ")" +
                "[\"highway\"];out body geom;";
    }

    private Single<OsmResponse> getOsmResponse(Location location) {
        OsmApiEndpoint selectedEndpoint;
        if (Math.random() < 0.7) {
            //Select the fastest endpoint most of the time
            selectedEndpoint = osmOverpassApis.get(0);
        } else {
            //Select a random endpoint 30% of the time to "correct" for anomalies
            selectedEndpoint = osmOverpassApis.get((int) (Math.random() * osmOverpassApis.size()));
        }
        return selectedEndpoint.getService()
                .getOsm(buildQueryBody(location))
                .doOnSubscribe(() -> logOsmRequest(selectedEndpoint))
                .doOnError((throwable) -> {
                    //catch any errors
                    updateEndpointTimeTaken(Integer.MAX_VALUE, selectedEndpoint);
                    logOsmError(selectedEndpoint, throwable);
                });
    }

    @Override
    public Observable<LimitResponse> getSpeedLimit(final Location location, LimitResponse lastResponse) {
        return getOsmResponse(location)
                .flatMapObservable(osmApi -> {
                    if (osmApi == null) {
                        return Observable.error(new Exception("OSM null response"));
                    }

                    LimitResponse.Builder builder = LimitResponse.builder()
                            .setOrigin(LimitResponse.ORIGIN_OSM)
                            .setTimestamp(System.currentTimeMillis());

                    Observable<LimitResponse> emptyObservableReponse = Observable.defer(() -> Observable.just(
                            builder.setDebugInfo("\nOSM info:\n--" + TextUtils.join("\n--", osmOverpassApis))
                                    .initDebugInfo(context)
                                    .build())
                    );

                    List<Element> elements = osmApi.getElements();

                    if (elements.isEmpty()) {
                        return emptyObservableReponse;
                    }

                    Element bestMatch = getBestElement(elements, lastResponse);
                    LimitResponse bestResponse = null;

                    for (Element element : elements) {

                        //Get coords
                        if (element.getGeometry() != null && !element.getGeometry().isEmpty()) {
                            builder.setCoords(element.getGeometry());
                        } else if (element != bestMatch) {
                            /* If coords are empty and element is not the best one,
                            no need to continue parsing info for cache. Skip to next element. */
                            continue;
                        }

                        //Get road names
                        Tags tags = element.getTags();
                        builder.setRoadName(parseOsmRoadName(tags));

                        //Get speed limit
                        String maxspeed = tags.getMaxspeed();
                        if (maxspeed != null) {
                            builder.setSpeedLimit(parseOsmSpeedLimit(maxspeed));
                        }

                        LimitResponse response = builder.initDebugInfo(context).build();

                        //Cache
                        cache.put(response);

                        if (element == bestMatch) {
                            bestResponse = response;
                        }
                    }

                    if (bestResponse != null) {
                        return Observable.just(bestResponse);
                    }

                    return emptyObservableReponse;
                })
                .onErrorReturn(throwable ->
                        LimitResponse.builder()
                                .setError(throwable)
                                .setTimestamp(System.currentTimeMillis())
                                .setOrigin(LimitResponse.ORIGIN_OSM)
                                .setDebugInfo("\nOSM Info:\n--" + TextUtils.join("\n--", osmOverpassApis))
                                .initDebugInfo(context)
                                .build()
                );
    }

    private String parseOsmRoadName(Tags tags) {
        String ref = tags.getRef();
        String name = tags.getName();
        if (ref == null && name == null) {
            return "null";
        } else if (name != null && ref != null) {
            return ref + " " + name;
        } else if (name != null) {
            return name;
        } else {
            return ref;
        }
    }

    private int parseOsmSpeedLimit(String maxspeed) {
        int speedLimit = -1;
        if (maxspeed.matches("^-?\\d+$")) {
            //If it is an integer, it is in km/h
            speedLimit = Integer.valueOf(maxspeed);
        } else if (maxspeed.contains("mph")) {
            String[] split = maxspeed.split(" ");
            speedLimit = Integer.valueOf(split[0]);
            speedLimit = Utils.convertMphToKmh(speedLimit);
        }

        return speedLimit;
    }

    private Element getBestElement(List<Element> elements, LimitResponse lastResponse) {
        Element bestElement = null;
        Element fallback = null;

        if (lastResponse != null) {
            for (Element newElement : elements) {
                Tags newTags = newElement.getTags();
                if (fallback == null && newTags.getMaxspeed() != null) {
                    fallback = newElement;
                }
                if (lastResponse.roadName().equals(parseOsmRoadName(newTags))) {
                    bestElement = newElement;
                    break;
                }
            }
        }

        if (bestElement == null) {
            bestElement = fallback != null ? fallback : elements.get(0);
        }
        return bestElement;
    }

    private void logOsmRequest(OsmApiEndpoint endpoint) {
        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logCustom(new CustomEvent("OSM Request")
                    .putCustomAttribute("Server", endpoint.getBaseUrl()));

            String endpointString = Uri.parse(endpoint.getBaseUrl()).getAuthority()
                    .replace(".", "_")
                    .replace("-", "_");
            String key = "osm_request_" + endpointString;
            FirebaseAnalytics.getInstance(context).logEvent(key, new Bundle());
        }
    }

    private void logOsmError(OsmApiEndpoint endpoint, Throwable throwable) {
        if (!BuildConfig.DEBUG) {
            if (throwable instanceof IOException) {
                Answers.getInstance().logCustom(new CustomEvent("Network Error")
                        .putCustomAttribute("Server", endpoint.getBaseUrl())
                        .putCustomAttribute("Message", throwable.getMessage()));

                String endpointString = Uri.parse(endpoint.getBaseUrl()).getAuthority()
                        .replace(".", "_")
                        .replace("-", "_");
                String key = "osm_error_" + endpointString;
                FirebaseAnalytics.getInstance(context).logEvent(key, new Bundle());
            }

            Crashlytics.logException(throwable);
        }
    }

}
