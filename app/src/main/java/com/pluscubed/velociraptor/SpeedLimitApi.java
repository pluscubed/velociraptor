package com.pluscubed.velociraptor;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.pluscubed.velociraptor.hereapi.HereService;
import com.pluscubed.velociraptor.hereapi.Link;
import com.pluscubed.velociraptor.hereapi.LinkInfo;
import com.pluscubed.velociraptor.osmapi.Element;
import com.pluscubed.velociraptor.osmapi.OsmApiEndpoint;
import com.pluscubed.velociraptor.osmapi.OsmResponse;
import com.pluscubed.velociraptor.osmapi.OsmService;
import com.pluscubed.velociraptor.osmapi.Tags;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Observable;
import rx.Single;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class SpeedLimitApi {

    private static final String HERE_ROUTING_API = "https://route.api.here.com/routing/7.2/";
    private static final String[] PUBLIC_OVERPASS_APIS = new String[]{
            "http://api.openstreetmap.fr/oapi/",
            "http://overpass.osm.rambler.ru/cgi/",
            "http://overpass-api.de/api/"
    };
    private final List<OsmApiEndpoint> mOsmOverpassApis;
    private Context mContext;
    private OsmService mOsmService;
    private HereService mHereService;
    private OsmApiSelectionInterceptor mOsmApiSelectionInterceptor;

    private int mHereTimeTaken;
    private String[] mLastOsmRoadNames;

    public SpeedLimitApi(Context context) {
        mContext = context;

        mOsmOverpassApis = Collections.synchronizedList(new ArrayList<OsmApiEndpoint>());
        synchronized (mOsmOverpassApis) {
            for (String api : PUBLIC_OVERPASS_APIS) {
                mOsmOverpassApis.add(new OsmApiEndpoint(api));
            }
            Collections.shuffle(mOsmOverpassApis);

            String[] stringArray = context.getResources().getStringArray(R.array.overpass_apis);
            for (int i = 0; i < stringArray.length; i++) {
                String api = stringArray[i];
                OsmApiEndpoint endpoint = new OsmApiEndpoint(api);
                endpoint.name = "velociraptor-server" + (i + 1);
                mOsmOverpassApis.add(0, endpoint);
            }
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor);
        }
        OkHttpClient client = builder.build();

        OkHttpClient hereClient = client.newBuilder().addInterceptor(new HereTimeTakenInterceptor()).build();
        Retrofit hereRest = buildRetrofit(hereClient, HERE_ROUTING_API);
        mHereService = hereRest.create(HereService.class);

        mOsmApiSelectionInterceptor = new OsmApiSelectionInterceptor();
        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(mOsmApiSelectionInterceptor)
                .build();
        Retrofit osmRest = buildRetrofit(osmClient, mOsmOverpassApis.get(0).baseUrl);
        mOsmService = osmRest.create(OsmService.class);
    }

    public String getApiInformation() {
        String text = "";
        synchronized (mOsmOverpassApis) {
            for (OsmApiEndpoint endpoint : mOsmOverpassApis) {
                text += (endpoint.toString() + "\n");
            }
        }
        text += "HERE - " + mHereTimeTaken + "ms\n";
        return text;
    }

    public Single<ApiResponse> getSpeedLimit(Location location) {
        return getOsmSpeedLimit(location)
                .switchIfEmpty(getHereSpeedLimit(location).toObservable()).toSingle();
    }

    private String getOsmQuery(Location location) {
        return "[out:json];" +
                "way(around:25,"
                + location.getLatitude() + ","
                + location.getLongitude() +
                ")" +
                "[\"highway\"];out;";
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

    private Observable<ApiResponse> getOsmSpeedLimit(final Location location) {
        final List<OsmApiEndpoint> endpoints = new ArrayList<>(mOsmOverpassApis);
        mOsmApiSelectionInterceptor.setApi(endpoints.get(0));

        return mOsmService.getOsm(getOsmQuery(location))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        logOsmRequest(mOsmApiSelectionInterceptor.api);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (!BuildConfig.DEBUG) {
                            if (throwable instanceof IOException) {
                                Answers.getInstance().logCustom(new CustomEvent("Network Error")
                                        .putCustomAttribute("Server", mOsmApiSelectionInterceptor.api.baseUrl));
                            } else {
                                Crashlytics.logException(throwable);
                            }
                        }

                        mOsmApiSelectionInterceptor.api.timeTaken = Integer.MAX_VALUE;
                        synchronized (mOsmOverpassApis) {
                            Collections.shuffle(mOsmOverpassApis);
                            Collections.sort(mOsmOverpassApis);
                        }
                    }
                })
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        if (integer <= 2) {
                            mOsmApiSelectionInterceptor.setApi(endpoints.get(integer));
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .flatMapObservable(new Func1<OsmResponse, Observable<ApiResponse>>() {
                    @Override
                    public Observable<ApiResponse> call(OsmResponse osmApi) {
                        if (osmApi == null) {
                            return Observable.error(new Exception("OSM null response"));
                        }

                        ApiResponse response = new ApiResponse();
                        response.useHere = false;

                        boolean useMetric = PrefUtils.getUseMetric(mContext);

                        List<Element> elements = osmApi.getElements();
                        if (!elements.isEmpty()) {
                            Element element = null;
                            Element fallback = null;

                            if (mLastOsmRoadNames != null) {
                                for (Element newElement : elements) {
                                    Tags newTags = newElement.getTags();
                                    if (newTags.getMaxspeed() != null) {
                                        fallback = newElement;
                                    }
                                    if (mLastOsmRoadNames[0] != null && mLastOsmRoadNames[0].equals(newTags.getRef()) ||
                                            mLastOsmRoadNames[1] != null && mLastOsmRoadNames[1].equals(newTags.getName())) {
                                        element = newElement;
                                        break;
                                    }
                                }
                            }

                            if (element == null) {
                                element = fallback != null ? fallback : elements.get(0);
                            }

                            Tags tags = element.getTags();
                            response.roadNames = new String[]{tags.getRef(), tags.getName()};
                            mLastOsmRoadNames = response.roadNames;
                            String maxspeed = tags.getMaxspeed();
                            if (maxspeed != null) {
                                if (maxspeed.matches("^-?\\d+$")) {
                                    //is integer -> km/h
                                    response.speedLimit = Integer.valueOf(maxspeed);
                                    if (!useMetric) {
                                        response.speedLimit = (int) (response.speedLimit / 1.609344 + 0.5d);
                                    }
                                } else if (maxspeed.contains("mph")) {
                                    String[] split = maxspeed.split(" ");
                                    response.speedLimit = Integer.valueOf(split[0]);
                                    if (useMetric) {
                                        response.speedLimit = (int) (response.speedLimit * 1.609344 + 0.5d);
                                    }
                                }
                                if (response.speedLimit != -1) {
                                    return Observable.just(response);
                                }
                            }
                        }
                        return Observable.empty();
                    }
                });
    }

    private void logOsmRequest(OsmApiEndpoint endpoint) {
        if (!BuildConfig.DEBUG)
            Answers.getInstance().logCustom(new CustomEvent("OSM Request")
                    .putCustomAttribute("Server", endpoint.baseUrl));
    }

    private Single<ApiResponse> getHereSpeedLimit(final Location location) {
        final String query = location.getLatitude() + "," + location.getLongitude();
        return mHereService.getLinkInfo(query, mContext.getString(R.string.here_app_id), mContext.getString(R.string.here_app_code))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (!BuildConfig.DEBUG)
                            Answers.getInstance().logCustom(new CustomEvent("HERE Request"));
                    }
                })
                .map(new Func1<LinkInfo, ApiResponse>() {
                    @Override
                    public ApiResponse call(LinkInfo linkInfo) {
                        ApiResponse response = new ApiResponse();
                        response.useHere = true;
                        if (linkInfo != null) {
                            Link link = linkInfo.getResponse().getLink().get(0);
                            if (link.getAddress() != null) {
                                response.roadNames = new String[]{link.getAddress().getLabel(),
                                        link.getAddress().getStreet()};
                            }
                            if (link.getSpeedLimit() != null && link.getSpeedLimit() != 0) {
                                double factor = PrefUtils.getUseMetric(mContext) ? 3.6 : 2.23;
                                response.speedLimit = (int) (link.getSpeedLimit() * factor + 0.5d);
                            }
                        }
                        return response;
                    }
                });
    }

    class ApiResponse {
        boolean useHere;
        //-1 if DNE
        int speedLimit = -1;
        String[] roadNames;
    }

    final class HereTimeTakenInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long start = System.currentTimeMillis();
            try {
                return chain.proceed(request);
            } finally {
                mHereTimeTaken = (int) (System.currentTimeMillis() - start);
            }
        }
    }

    final class OsmApiSelectionInterceptor implements Interceptor {
        private volatile OsmApiEndpoint api;

        public void setApi(OsmApiEndpoint api) {
            this.api = api;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            List<String> pathSegments = request.url().pathSegments();
            String url = api.baseUrl + pathSegments.get(pathSegments.size() - 1) + "?" + request.url().encodedQuery();
            HttpUrl newUrl = HttpUrl.parse(url);
            request = request.newBuilder()
                    .url(newUrl)
                    .addHeader("User-Agent", "Velociraptor/" + BuildConfig.VERSION_NAME)
                    .build();

            long start = System.currentTimeMillis();
            try {
                Response proceed = chain.proceed(request);
                if (!proceed.isSuccessful()) {
                    throw new IOException(proceed.toString());
                } else {
                    api.timeTaken = (int) (System.currentTimeMillis() - start);
                }
                return proceed;
            } finally {
                api.timeTakenTimestamp = System.currentTimeMillis();
                synchronized (mOsmOverpassApis) {
                    Collections.sort(mOsmOverpassApis);
                }
            }
        }
    }
}
