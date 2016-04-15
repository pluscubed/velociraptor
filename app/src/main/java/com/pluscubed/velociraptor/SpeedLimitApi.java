package com.pluscubed.velociraptor;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.pluscubed.velociraptor.hereapi.HereService;
import com.pluscubed.velociraptor.hereapi.Link;
import com.pluscubed.velociraptor.hereapi.LinkInfo;
import com.pluscubed.velociraptor.osmapi.Element;
import com.pluscubed.velociraptor.osmapi.OsmApiEndpoint;
import com.pluscubed.velociraptor.osmapi.OsmResponse;
import com.pluscubed.velociraptor.osmapi.OsmService;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import rx.schedulers.Schedulers;

public class SpeedLimitApi {

    private static final String HERE_ROUTING_API = "http://route.st.nlp.nokia.com/routing/6.2/";
    private static final String[] OSM_OVERPASS_APIS = new String[]{
            "http://api.openstreetmap.fr/oapi/",
            "http://overpass.osm.rambler.ru/cgi/",
            "http://overpass-api.de/api/"
    };

    private Context mContext;

    private OsmService mOsmService;
    private HereService mHereService;
    private OsmApiSelectionInterceptor mOsmApiSelectionInterceptor;

    private List<OsmApiEndpoint> mOsmOverpassApis;

    public SpeedLimitApi(Context context) {
        mContext = context;

        mOsmOverpassApis = new ArrayList<>();
        for (String api : OSM_OVERPASS_APIS) {
            mOsmOverpassApis.add(new OsmApiEndpoint(api));
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor);
        }
        OkHttpClient client = builder.build();
        Retrofit hereRest = buildRetrofit(client, HERE_ROUTING_API);
        mHereService = hereRest.create(HereService.class);

        mOsmApiSelectionInterceptor = new OsmApiSelectionInterceptor();
        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(mOsmApiSelectionInterceptor)
                .connectTimeout(2000, TimeUnit.MILLISECONDS)
                .readTimeout(5000, TimeUnit.MILLISECONDS)
                .build();
        Retrofit osmRest = buildRetrofit(osmClient, OSM_OVERPASS_APIS[0]);
        mOsmService = osmRest.create(OsmService.class);
        ;
    }

    public Single<Integer> getSpeedLimit(Location location) {
        return getOsmSpeedLimit(location)
                //.switchIfEmpty(getHereSpeedLimit(location).toObservable())
                .defaultIfEmpty(null)
                .toSingle();
    }

    private String getOsmQuery(Location location) {
        return "[out:json];" +
                "way(around:25,"
                + location.getLatitude() + ","
                + location.getLongitude() +
                ")" +
                "[\"highway\"][\"maxspeed\"];out;";
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

    private Observable<Integer> getOsmSpeedLimit(final Location location) {
        final List<OsmApiEndpoint> endpoints = new ArrayList<>(mOsmOverpassApis);
        return mOsmService.getOsm(getOsmQuery(location))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        logOsmRequest();
                        mOsmApiSelectionInterceptor.setApi(endpoints.get(0));
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Single<? extends OsmResponse>>() {
                    @Override
                    public Single<? extends OsmResponse> call(Throwable throwable) {
                        logOsmRequest();
                        mOsmApiSelectionInterceptor.setApi(endpoints.get(1));
                        return mOsmService.getOsm(getOsmQuery(location));
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Single<? extends OsmResponse>>() {
                    @Override
                    public Single<? extends OsmResponse> call(Throwable throwable) {
                        logOsmRequest();
                        mOsmApiSelectionInterceptor.setApi(endpoints.get(2));
                        return mOsmService.getOsm(getOsmQuery(location));
                    }
                })
                .onErrorResumeNext(Single.just((OsmResponse) null))
                .flatMapObservable(new Func1<OsmResponse, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(OsmResponse osmApi) {
                        if (osmApi == null) {
                            return Observable.empty();
                        }

                        boolean useMetric = PrefUtils.getUseMetric(mContext);

                        List<Element> elements = osmApi.getElements();
                        if (!elements.isEmpty()) {
                            Element element = elements.get(0);
                            String maxspeed = element.getTags().getMaxspeed();
                            if (maxspeed.matches("^-?\\d+$")) {
                                //is integer -> km/h
                                Integer limit = Integer.valueOf(maxspeed);
                                if (!useMetric) {
                                    limit = (int) (limit / 1.609344);
                                }
                                return Observable.just(limit);
                            } else if (maxspeed.contains("mph")) {
                                String[] split = maxspeed.split(" ");
                                Integer limit = Integer.valueOf(split[0]);
                                if (useMetric) {
                                    limit = (int) (limit * 1.609344);
                                }
                                return Observable.just(limit);
                            }
                        }
                        return Observable.empty();
                    }
                });
    }

    private void logOsmRequest() {
        if (!BuildConfig.DEBUG)
            Answers.getInstance().logCustom(new CustomEvent("OSM Request"));
    }

    private Single<Integer> getHereSpeedLimit(final Location location) {
        final String query = location.getLatitude() + "," + location.getLongitude();
        return mHereService.getLinkInfo(query, mContext.getString(R.string.here_app_id), mContext.getString(R.string.here_app_code))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(new Action1<LinkInfo>() {
                    @Override
                    public void call(LinkInfo linkInfo) {
                        if (linkInfo != null && !BuildConfig.DEBUG)
                            Answers.getInstance().logCustom(new CustomEvent("HERE Request"));
                    }
                })
                .map(new Func1<LinkInfo, Integer>() {
                    @Override
                    public Integer call(LinkInfo linkInfo) {
                        if (linkInfo != null) {
                            Link link = linkInfo.getResponse().getLink().get(0);
                            Double speedLimit = link.getSpeedLimit();
                            if (speedLimit != null) {
                                double factor = PrefUtils.getUseMetric(mContext) ? 3.6 : 2.23;
                                return (int) (speedLimit * factor + 0.5d);
                            }
                        }
                        return null;
                    }
                });
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
                    .build();

            long start = System.currentTimeMillis();
            try {
                return chain.proceed(request);
            } finally {
                api.timeTaken = (int) (System.currentTimeMillis() - start);
                Collections.sort(mOsmOverpassApis);
            }
        }
    }
}
