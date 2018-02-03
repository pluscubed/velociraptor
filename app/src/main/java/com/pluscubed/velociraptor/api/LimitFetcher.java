package com.pluscubed.velociraptor.api;

import android.content.Context;
import android.location.Location;

import com.android.billingclient.api.Purchase;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.api.osm.OsmLimitProvider;
import com.pluscubed.velociraptor.api.raptor.RaptorLimitProvider;
import com.pluscubed.velociraptor.cache.LimitCache;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import rx.Observable;
import rx.Single;

public class LimitFetcher {

    private Context context;

    private OsmLimitProvider osmLimitProvider;
    private RaptorLimitProvider raptorLimitProvider;

    private LimitResponse lastResponse;
    private LimitResponse lastNetworkResponse;

    public LimitFetcher(Context context) {
        this.context = context;

        OkHttpClient client = buildOkHttpClient();

        LimitCache cache = LimitCache.getInstance(context);
        this.osmLimitProvider = new OsmLimitProvider(context, client, cache);
        this.raptorLimitProvider = new RaptorLimitProvider(context, client, cache);
    }

    public static Retrofit buildRetrofit(OkHttpClient client, String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }

    private OkHttpClient buildOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }
        return builder.build();
    }

    public void verifyRaptorService(Purchase purchase) {
        raptorLimitProvider.verify(purchase);
    }

    public Single<LimitResponse> getSpeedLimit(Location location) {
        Observable<LimitResponse> cacheQuery =
                LimitCache.getInstance(context).getSpeedLimit(location, lastResponse);
        return cacheQuery.toList()
                .flatMap(cacheResponses -> {
                    LimitResponse cacheResponse = cacheResponses.get(0);

                    Observable<LimitResponse> chain = Observable.just(cacheResponse);

                    if (cacheResponse.speedLimit() == -1 && Utils.isNetworkConnected(context)) {
                        Observable<LimitResponse> raptorQuery =
                                raptorLimitProvider.getSpeedLimit(location, lastResponse);

                        // Delay network query if the last response was received less than 5 seconds ago
                        if (lastNetworkResponse != null) {
                            long delay = 5000 - (System.currentTimeMillis() - lastNetworkResponse.timestamp());
                            raptorQuery = raptorQuery.delaySubscription(delay, TimeUnit.MILLISECONDS);
                        }

                        // 1. Always try raptor service if cache didn't hit / didn't contain a limit
                        chain = chain.concatWith(raptorQuery);

                        // 2. Try OSM if the cache hits didn't contain a limit BUT were not from OSM
                        //    i.e. query OSM as it might have the limit
                        boolean cachedOsmWithNoLimit = false;
                        for (LimitResponse cacheRes : cacheResponses) {
                            if (cacheRes.origin() == LimitResponse.ORIGIN_OSM) {
                                cachedOsmWithNoLimit = true;
                                break;
                            }
                        }
                        if (!cachedOsmWithNoLimit) {
                            Observable<LimitResponse> osmQuery = osmLimitProvider.getSpeedLimit(location, lastResponse);
                            chain = chain.concatWith(osmQuery);
                        }
                    }

                    return chain;
                })
                //Keep taking responses until one has a speed limit (or tried all)
                .takeUntil(limitResponse -> limitResponse.speedLimit() != -1)
                //Accumulate debug infos, based on the last response (the one with the speed limit or the last option)
                .map(LimitResponse::toBuilder)
                .reduce((acc, builder) -> builder.setDebugInfo(acc.debugInfo() + "\n" + builder.debugInfo()))
                .map(LimitResponse.Builder::build)
                .toSingle()
                //Record the last response's timestamp and network response
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

}
