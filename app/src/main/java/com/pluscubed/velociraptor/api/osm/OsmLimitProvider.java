package com.pluscubed.velociraptor.api.osm;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.LimitFetcher;
import com.pluscubed.velociraptor.api.LimitInterceptor;
import com.pluscubed.velociraptor.api.LimitProvider;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.api.osm.data.Element;
import com.pluscubed.velociraptor.api.osm.data.OsmResponse;
import com.pluscubed.velociraptor.api.osm.data.Tags;
import com.pluscubed.velociraptor.cache.LimitCache;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Single;

public class OsmLimitProvider implements LimitProvider {

    public static final int OSM_RADIUS = 15;
    public static final String FIREBASE_CONFIG_OSM = "single_osm_api";

    private Context context;
    private OsmService osmService;

    private String osmOverpassApi;

    public OsmLimitProvider(Context context, OkHttpClient client) {
        this.context = context;

        FirebaseRemoteConfig instance = FirebaseRemoteConfig.getInstance();
        instance.fetch().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                instance.activateFetched();
                osmOverpassApi = instance.getString(FIREBASE_CONFIG_OSM);
            } else {
                osmOverpassApi = context.getString(R.string.overpass_api);
            }

            LimitInterceptor osmInterceptor = new LimitInterceptor(new LimitInterceptor.Callback());
            OkHttpClient osmClient = client.newBuilder()
                    .addInterceptor(osmInterceptor)
                    .build();
            Retrofit osmRest = LimitFetcher.buildRetrofit(osmClient, osmOverpassApi);
            osmService = osmRest.create(OsmService.class);

        });
    }


    private String getOsmQuery(Location location) {
        return "[out:json];" +
                "way(around:" + OSM_RADIUS + ","
                + location.getLatitude() + ","
                + location.getLongitude() +
                ")" +
                "[\"highway\"];out body geom;";
    }

    @Override
    public Observable<LimitResponse> getSpeedLimit(final Location location, LimitResponse lastResponse) {
        return getOsmResponse(location)
                .flatMapObservable(osmApi -> {
                    if (osmApi == null) {
                        return Observable.error(new Exception("OSM null response"));
                    }

                    boolean useMetric = PrefUtils.getUseMetric(context);

                    List<Element> elements = osmApi.getElements();

                    if (elements.isEmpty()) {
                        return Observable.empty();
                    }

                    Element bestMatch = getBestElement(elements, lastResponse);
                    LimitResponse bestResponse = null;

                    for (Element element : elements) {
                        LimitResponse.Builder responseBuilder = LimitResponse.builder();

                        //Get coords
                        if (element.getGeometry() != null && !element.getGeometry().isEmpty()) {
                            responseBuilder.setCoords(element.getGeometry());
                        } else if (element != bestMatch) {
                            /* If coords are empty and element is not the best one,
                            no need to continue parsing info for cache. Skip to next element. */
                            continue;
                        }

                        responseBuilder
                                .setTimestamp(System.currentTimeMillis())
                                .setOrigin(LimitResponse.ORIGIN_OSM);

                        //Get road names
                        Tags tags = element.getTags();
                        responseBuilder.setRoadName(parseOsmRoadName(tags));

                        //Get speed limit
                        String maxspeed = tags.getMaxspeed();
                        if (maxspeed != null) {
                            responseBuilder.setSpeedLimit(parseOsmSpeedLimit(useMetric, maxspeed));
                        }

                        LimitResponse response = responseBuilder.build();

                        //Cache
                        LimitCache.getInstance(context).put(response);

                        if (element == bestMatch) {
                            bestResponse = response;
                        }
                    }

                    if (bestResponse != null) {
                        return Observable.just(bestResponse);
                    }

                    return Observable.empty();
                });
    }

    private Single<OsmResponse> getOsmResponse(Location location) {
        return osmService.getOsm(getOsmQuery(location))
                .doOnSubscribe(() -> logOsmRequest(osmOverpassApi))
                .doOnError((throwable) -> logOsmError(osmOverpassApi, throwable));
    }

    private String parseOsmRoadName(Tags tags) {
        return tags.getRef() + " - " + tags.getName();
    }

    private int parseOsmSpeedLimit(boolean useMetric, String maxspeed) {
        int speedLimit = -1;
        if (maxspeed.matches("^-?\\d+$")) {
            //is integer -> km/h
            speedLimit = Integer.valueOf(maxspeed);
            if (!useMetric) {
                speedLimit = (int) (speedLimit / 1.609344 + 0.5d);
            }
        } else if (maxspeed.contains("mph")) {
            String[] split = maxspeed.split(" ");
            speedLimit = Integer.valueOf(split[0]);
            if (useMetric) {
                speedLimit = (int) (speedLimit * 1.609344 + 0.5d);
            }
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

    private void logOsmRequest(String endpoint) {
        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logCustom(new CustomEvent("OSM Request")
                    .putCustomAttribute("Server", endpoint));

            Bundle bundle = new Bundle();
            String key = "osm_request_" + Uri.parse(endpoint).getAuthority()
                    .replace(".", "_")
                    .replace("-", "_");
            FirebaseAnalytics.getInstance(context).logEvent(key, bundle);
        }
    }

    private void logOsmError(String endpoint, Throwable throwable) {
        if (!BuildConfig.DEBUG) {
            if (throwable instanceof IOException) {
                Answers.getInstance().logCustom(new CustomEvent("Network Error")
                        .putCustomAttribute("Server", osmOverpassApi)
                        .putCustomAttribute("Message", throwable.getMessage()));
            }

            Crashlytics.logException(throwable);
        }
    }

}
