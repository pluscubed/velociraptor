package com.pluscubed.velociraptor.api;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.pluscubed.velociraptor.api.osmapi.Element;
import com.pluscubed.velociraptor.api.osmapi.OsmResponse;
import com.pluscubed.velociraptor.api.osmapi.OsmService;
import com.pluscubed.velociraptor.api.osmapi.Tags;
import com.pluscubed.velociraptor.cache.LimitCache;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Observable;
import rx.Single;

public class LimitFetcher {

    public static final int OSM_RADIUS = 15;
    private final OsmApiEndpoint osmOverpassApi;

    private Context context;
    private OsmService osmService;

    private LimitResponse lastResponse;
    private LimitResponse lastNetworkResponse;

    public LimitFetcher(Context context) {
        this.context = context;

        String privateApiHost = context.getString(R.string.overpass_api);
        osmOverpassApi = new OsmApiEndpoint(privateApiHost, "velociraptor-server");

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }
        OkHttpClient client = builder.build();

        OsmInterceptor osmInterceptor = new OsmInterceptor(osmOverpassApi);
        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(osmInterceptor)
                .build();
        Retrofit osmRest = buildRetrofit(osmClient, osmOverpassApi.baseUrl);
        osmService = osmRest.create(OsmService.class);
    }

    public String getApiInformation() {
        return osmOverpassApi.toString();
    }

    public Single<LimitResponse> getSpeedLimit(Location location) {
        //TODO: Restructure as stream of error/missing/completed responses

        Observable<LimitResponse> query = getOsmSpeedLimit(location);

        //Delay query if the last response was received less than 5 seconds ago
        if (lastNetworkResponse != null) {
            long delay = 5000 - (System.currentTimeMillis() - lastNetworkResponse.timestamp());
            query = query.delaySubscription(delay, TimeUnit.MILLISECONDS);
        }

        query = query.defaultIfEmpty(LimitResponse.builder().build());
        String lastRoadName = lastResponse == null ? null : lastResponse.roadName();
        return LimitCache.getInstance(context)
                .get(lastRoadName, new Coord(location))
                .switchIfEmpty(query)
                .toSingle()
                .doOnSuccess(limitResponse -> {
                    if (limitResponse.timestamp() == 0) {
                        limitResponse = limitResponse.toBuilder()
                                .setTimestamp(System.currentTimeMillis())
                                .build();
                    }
                    lastResponse = limitResponse;
                    if (!limitResponse.fromCache()) {
                        lastNetworkResponse = limitResponse;
                    }
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

    @NonNull
    private Retrofit buildRetrofit(OkHttpClient client, String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }


    /**
     * Returns response (regardless of whether there is speed limit) and caches each way received,
     * or empty if there is no road information
     */
    private Observable<LimitResponse> getOsmSpeedLimit(final Location location) {
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

                    Element bestMatch = getBestElement(elements);
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

                        responseBuilder.setTimestamp(System.currentTimeMillis());

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
                .doOnError((throwable) -> {
                    if (!BuildConfig.DEBUG) {
                        if (throwable instanceof IOException) {
                            Answers.getInstance().logCustom(new CustomEvent("Network Error")
                                    .putCustomAttribute("Server", osmOverpassApi.baseUrl)
                                    .putCustomAttribute("Message", throwable.getMessage()));
                        }

                        Crashlytics.logException(throwable);
                    }
                });
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

    private Element getBestElement(List<Element> elements) {
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
                    .putCustomAttribute("Server", endpoint.baseUrl));
        }

        Bundle bundle = new Bundle();
        String key = "osm_request_" + Uri.parse(endpoint.baseUrl).getAuthority().replace(".", "_").replace("-", "_");
        FirebaseAnalytics.getInstance(context).logEvent(key, bundle);
    }

}
