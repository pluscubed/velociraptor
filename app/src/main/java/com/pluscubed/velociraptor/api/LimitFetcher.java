package com.pluscubed.velociraptor.api;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.hereapi.HereService;
import com.pluscubed.velociraptor.api.hereapi.Link;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.pluscubed.velociraptor.api.osmapi.Element;
import com.pluscubed.velociraptor.api.osmapi.OsmResponse;
import com.pluscubed.velociraptor.api.osmapi.OsmService;
import com.pluscubed.velociraptor.api.osmapi.Tags;
import com.pluscubed.velociraptor.cache.LimitCache;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class LimitFetcher {

    public static final String FB_CONFIG_OSM_APIS = "osm_apis";
    public static final String FB_CONFIG_OSM_API_ENABLED_PREFIX = "osm_api";
    public static final int OSM_RADIUS = 15;
    private static final String HERE_ROUTING_API = "https://route.api.here.com/routing/7.2/";
    private final List<OsmApiEndpoint> osmOverpassApis;
    private final ObjectMapper objectMapper;

    private Context context;
    private OsmService osmService;
    private HereService hereService;
    private OsmInterceptor osmInterceptor;

    private LimitResponse lastResponse;
    private LimitResponse lastNetworkResponse;

    public LimitFetcher(Context context) {
        this.context = context;

        objectMapper = new ObjectMapper();

        osmOverpassApis = Collections.synchronizedList(new ArrayList<OsmApiEndpoint>());

        String privateApiHost = context.getString(R.string.overpass_api);
        OsmApiEndpoint mainEndpoint = new OsmApiEndpoint(privateApiHost, true);
        mainEndpoint.name = "velociraptor-server";
        osmOverpassApis.add(mainEndpoint);

        try {
            addApis(FB_CONFIG_OSM_APIS);
        } catch (IOException ignored) {
        }

        updateEndpoints()
                .subscribeOn(Schedulers.io())
                .subscribe();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }
        OkHttpClient client = builder.build();

        OkHttpClient hereClient = client.newBuilder().build();
        Retrofit hereRest = buildRetrofit(hereClient, HERE_ROUTING_API);
        hereService = hereRest.create(HereService.class);

        osmInterceptor = new OsmInterceptor();
        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(osmInterceptor)
                .build();
        Retrofit osmRest = buildRetrofit(osmClient, osmOverpassApis.get(0).baseUrl);
        osmService = osmRest.create(OsmService.class);
    }

    private Completable updateEndpoints() {
        return Completable.fromEmitter(completableEmitter -> {
            FirebaseRemoteConfig.getInstance().fetch().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseRemoteConfig.getInstance().activateFetched();
                    completableEmitter.onCompleted();
                } else {
                    completableEmitter.onError(task.getException());
                }
            });
        }).retry(3).onErrorComplete()
                .andThen(Completable.create(completableSubscriber -> {
                    try {
                        addApis(FB_CONFIG_OSM_APIS);
                        completableSubscriber.onCompleted();
                    } catch (IOException e) {
                        completableSubscriber.onError(e);
                    }
                }));

    }

    private void addApis(String configKey) throws IOException {
        String apisString = FirebaseRemoteConfig.getInstance().getString(configKey);

        if (apisString == null || apisString.isEmpty()) {
            return;
        }

        String[] stringArray = objectMapper.readValue(apisString, new TypeReference<String[]>() {
        });

        synchronized (osmOverpassApis) {
            for (Iterator<OsmApiEndpoint> iterator = osmOverpassApis.iterator(); iterator.hasNext(); ) {
                OsmApiEndpoint endpoint = iterator.next();
                if (endpoint.name == null) {
                    iterator.remove();
                }
            }
            for (int i = 0; i < stringArray.length; i++) {
                String apiHost = stringArray[i];
                boolean publicEnabled = FirebaseRemoteConfig.getInstance()
                        .getBoolean(FB_CONFIG_OSM_API_ENABLED_PREFIX + i);
                OsmApiEndpoint endpoint = new OsmApiEndpoint(apiHost, publicEnabled);
                osmOverpassApis.add(endpoint);
            }
            Collections.shuffle(osmOverpassApis);
            Collections.sort(osmOverpassApis);
        }
    }

    public String getApiInformation() {
        String text = "";
        synchronized (osmOverpassApis) {
            for (OsmApiEndpoint endpoint : osmOverpassApis) {
                text += (endpoint.toString() + "\n");
            }
        }
        return text;
    }

    public Single<LimitResponse> getSpeedLimit(Location location) {
        //TODO: Restructure as stream of error/missing/completed responses

        Observable<LimitResponse> query;

        String[] hereCodes = PrefUtils.getHereCodes(context).split(",");
        if (hereCodes.length == 2) {
            query = getHereSpeedLimit(location);
        } else {
            query = getOsmSpeedLimit(location);
        }

        //Delay query if the last response was received less than 5 seconds ago
        if (lastNetworkResponse != null) {
            long delay = 5000 - (System.currentTimeMillis() - lastNetworkResponse.timestamp());
            query = query.delaySubscription(delay, TimeUnit.MILLISECONDS);
        }

        if (hereCodes.length == 2) {
            query = query
                    .onErrorReturn(throwable -> {
                        if (!BuildConfig.DEBUG) {
                            if (throwable instanceof IOException) {
                                Answers.getInstance().logCustom(new CustomEvent("Network Error")
                                        .putCustomAttribute("Server", "HERE")
                                        .putCustomAttribute("Message", throwable.getMessage()));
                            }
                            Crashlytics.logException(throwable);
                        }
                        return LimitResponse.builder().build();
                    })
                    .flatMap(limitResponse -> {
                        if (limitResponse.speedLimit() == -1) {
                            return getOsmSpeedLimit(location);
                        } else {
                            return Observable.just(limitResponse);
                        }
                    });
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
        final List<OsmApiEndpoint> endpoints = new ArrayList<>(osmOverpassApis);

        for (Iterator<OsmApiEndpoint> iterator = endpoints.iterator(); iterator.hasNext(); ) {
            OsmApiEndpoint endpoint = iterator.next();
            if (!endpoint.enabled) {
                iterator.remove();
            }
        }

        osmInterceptor.setEndpoint(endpoints.get(0));

        return getOsmResponse(location, endpoints)
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

    private Single<OsmResponse> getOsmResponse(Location location, final List<OsmApiEndpoint> endpoints) {
        return osmService.getOsm(getOsmQuery(location))
                .doOnSubscribe(() -> logOsmRequest(osmInterceptor.getEndpoint()))
                .doOnEach(notification -> {
                    synchronized (osmOverpassApis) {
                        Collections.sort(osmOverpassApis);
                    }
                })
                .retry((count, throwable) -> {
                    if (!BuildConfig.DEBUG) {
                        if (throwable instanceof IOException) {
                            Answers.getInstance().logCustom(new CustomEvent("Network Error")
                                    .putCustomAttribute("Server", osmInterceptor.getEndpoint().baseUrl)
                                    .putCustomAttribute("Message", throwable.getMessage()));
                        }

                        Crashlytics.logException(throwable);
                    }

                    osmInterceptor.getEndpoint().timeTaken = Integer.MAX_VALUE;
                    synchronized (osmOverpassApis) {
                        Collections.shuffle(osmOverpassApis);
                        Collections.sort(osmOverpassApis);
                    }

                    if (count < endpoints.size()) {
                        OsmApiEndpoint endpoint = endpoints.get(count);

                        osmInterceptor.setEndpoint(endpoint);
                        return true;
                    }

                    return false;
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

    /**
     * Returns and caches response (regardless of whether there is speed limit),
     * or empty if there is no road information
     */
    private Observable<LimitResponse> getHereSpeedLimit(final Location location) {
        final String query = location.getLatitude() + "," + location.getLongitude();

        String[] array = PrefUtils.getHereCodes(context).split(",");

        return hereService.getLinkInfo(query, "roadName", array[0], array[1])
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(this::logHereRequest)
                .map(linkInfo -> {
                    LimitResponse.Builder responseBuilder = LimitResponse.builder()
                            .setFromHere(true);

                    if (linkInfo == null) {
                        return responseBuilder.build();
                    }

                    responseBuilder.setTimestamp(System.currentTimeMillis());

                    Link link = linkInfo.getResponse().getLink().get(0);
                    if (link.getRoadName() != null && !link.getRoadName().isEmpty()) {
                        responseBuilder.setRoadName(parseHereRoadName(link));
                    } else {
                        responseBuilder.setRoadName("null - null");
                    }
                    if (link.getSpeedLimit() != null && link.getSpeedLimit() != 0) {
                        double factor = PrefUtils.getUseMetric(context) ? 3.6 : 2.23;
                        responseBuilder.setSpeedLimit((int) (link.getSpeedLimit() * factor + 0.5d));
                    }
                    if (link.getShape().size() >= 2) {
                        List<Coord> coordsList = new ArrayList<>();
                        for (String coords : link.getShape()) {
                            String[] coordsSplit = coords.split(",");
                            double lat = Double.parseDouble(coordsSplit[0]);
                            double lon = Double.parseDouble(coordsSplit[1]);
                            Coord coord = new Coord(lat, lon);
                            coordsList.add(coord);
                        }
                        responseBuilder.setCoords(coordsList);
                    }

                    LimitResponse response = responseBuilder.build();
                    LimitCache.getInstance(context).put(response);

                    return response;
                }).toObservable();
    }

    @NonNull
    private String parseHereRoadName(Link link) {
        return "null - " + link.getRoadName();
    }

    private void logHereRequest() {
        if (!BuildConfig.DEBUG) {
            Answers.getInstance().logCustom(new CustomEvent("HERE Request"));
        }

        Bundle bundle = new Bundle();
        String key = "here_request";
        FirebaseAnalytics.getInstance(context).logEvent(key, bundle);
    }

}
