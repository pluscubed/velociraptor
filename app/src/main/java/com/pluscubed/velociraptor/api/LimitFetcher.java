package com.pluscubed.velociraptor.api;

import android.content.Context;
import android.location.Location;

import com.android.billingclient.api.Purchase;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.api.osm.OsmLimitProvider;
import com.pluscubed.velociraptor.api.raptor.RaptorLimitProvider;
import com.pluscubed.velociraptor.cache.LimitCache;

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

        this.osmLimitProvider = new OsmLimitProvider(context, client);
        this.raptorLimitProvider = new RaptorLimitProvider(context, client, LimitCache.getInstance(context));
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
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
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
        //TODO: Restructure as stream of error/missing/completed responses

        String lastRoadName = lastResponse == null ? null : lastResponse.roadName();
        Observable<LimitResponse> cacheQuery = LimitCache.getInstance(context)
                .get(lastRoadName, new Coord(location));

        Observable<LimitResponse> raptorQuery =
                raptorLimitProvider.getSpeedLimit(location, lastResponse);

        Observable<LimitResponse> osmQuery =
                osmLimitProvider.getSpeedLimit(location, lastResponse);

        //Delay network query if the last response was received less than 5 seconds ago
        if (lastNetworkResponse != null) {
            long delay = 5000 - (System.currentTimeMillis() - lastNetworkResponse.timestamp());
            raptorQuery = raptorQuery.delaySubscription(delay, TimeUnit.MILLISECONDS);
        }

        Observable<LimitResponse> finalRaptorQuery = raptorQuery;
        return cacheQuery.defaultIfEmpty(null)
                .switchMap(limitResponse -> {
                    if (limitResponse == null) {
                        return finalRaptorQuery
                                .switchIfEmpty(osmQuery)
                                .defaultIfEmpty(LimitResponse.builder().build());
                    }

                    if (limitResponse.origin() == LimitResponse.ORIGIN_OSM
                            && limitResponse.speedLimit() == -1) {
                        return finalRaptorQuery
                                .defaultIfEmpty(limitResponse);
                    }

                    return Observable.just(limitResponse);
                })
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

}
