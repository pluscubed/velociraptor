package com.pluscubed.velociraptor;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Pair;

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
import rx.Single;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class SpeedLimitApi {

    private static final String HERE_ROUTING_API = "http://route.st.nlp.nokia.com/routing/6.2/";
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

    private Tags mLastTags;

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
        Retrofit hereRest = buildRetrofit(client, HERE_ROUTING_API);
        mHereService = hereRest.create(HereService.class);

        mOsmApiSelectionInterceptor = new OsmApiSelectionInterceptor();
        OkHttpClient osmClient = client.newBuilder()
                .addInterceptor(mOsmApiSelectionInterceptor)
                .build();
        Retrofit osmRest = buildRetrofit(osmClient, mOsmOverpassApis.get(0).baseUrl);
        mOsmService = osmRest.create(OsmService.class);
    }

    public List<OsmApiEndpoint> getOsmOverpassApis() {
        return mOsmOverpassApis;
    }

    public Single<Pair<Integer, Tags>> getSpeedLimit(Location location) {
        return getOsmSpeedLimit(location);
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

    private Single<Pair<Integer, Tags>> getOsmSpeedLimit(final Location location) {
        final List<OsmApiEndpoint> endpoints = new ArrayList<>(mOsmOverpassApis);
        mOsmApiSelectionInterceptor.setApi(endpoints.get(0));
        return mOsmService.getOsm(getOsmQuery(location))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        logOsmRequest();
                    }
                })
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        throwable.printStackTrace();
                        if (!BuildConfig.DEBUG)
                            Crashlytics.logException(throwable);


                        endpoints.get(integer - 1).timeTaken = Integer.MAX_VALUE;
                        synchronized (mOsmOverpassApis) {
                            Collections.shuffle(mOsmOverpassApis);
                            Collections.sort(mOsmOverpassApis);
                        }

                        if (integer <= 2) {
                            mOsmApiSelectionInterceptor.setApi(endpoints.get(integer));
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .flatMap(new Func1<OsmResponse, Single<Pair<Integer, Tags>>>() {
                    @Override
                    public Single<Pair<Integer, Tags>> call(OsmResponse osmApi) {
                        if (osmApi == null) {
                            return Single.just(new Pair<>((Integer) null, (Tags) null));
                        }

                        boolean useMetric = PrefUtils.getUseMetric(mContext);

                        List<Element> elements = osmApi.getElements();
                        if (!elements.isEmpty()) {
                            Element element = null;

                            for (Element newElement : elements) {
                                Tags newTags = newElement.getTags();
                                if (newTags.getName() != null && newTags.getName().equals(mLastTags.getName())
                                        && newTags.getRef() != null && newTags.getRef().equals(mLastTags.getRef())) {
                                    element = newElement;
                                }
                            }
                            if (mLastTags == null || element == null) {
                                element = elements.get(0);
                            }

                            Tags tags = element.getTags();
                            mLastTags = tags;
                            String maxspeed = tags.getMaxspeed();
                            if (maxspeed.matches("^-?\\d+$")) {
                                //is integer -> km/h
                                Integer limit = Integer.valueOf(maxspeed);
                                if (!useMetric) {
                                    limit = (int) (limit / 1.609344);
                                }
                                return Single.just(new Pair<>(limit, tags));
                            } else if (maxspeed.contains("mph")) {
                                String[] split = maxspeed.split(" ");
                                Integer limit = Integer.valueOf(split[0]);
                                if (useMetric) {
                                    limit = (int) (limit * 1.609344);
                                }
                                return Single.just(new Pair<>(limit, tags));
                            }
                            return Single.just(new Pair<>((Integer) null, tags));
                        }
                        return Single.just(new Pair<>((Integer) null, (Tags) null));
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
