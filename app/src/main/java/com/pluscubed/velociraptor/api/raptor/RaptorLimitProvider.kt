package com.pluscubed.velociraptor.api.raptor


import android.content.Context
import android.location.Location
import com.android.billingclient.api.Purchase
import com.google.maps.android.PolyUtil
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.api.*
import com.pluscubed.velociraptor.billing.BillingConstants
import com.pluscubed.velociraptor.cache.LimitCache
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*

class RaptorLimitProvider(context: Context, client: OkHttpClient, limitCache: LimitCache) : LimitProvider {

    private val raptorService: RaptorService
    private val limitCache: LimitCache

    private var id: String

    private var hereToken: String
    private var tomtomToken: String

    private val SERVER_URL = "http://overpass.pluscubed.com:4000/"
    private val DEBUG = BuildConfig.BUILD_TYPE.equals("debug")

    init {
        val interceptor = LimitInterceptor(LimitInterceptor.Callback())
        val raptorClient = client.newBuilder()
                .addInterceptor(interceptor)
                .build()
        val raptorRest = LimitFetcher.buildRetrofit(raptorClient, SERVER_URL)
        raptorService = raptorRest.create(RaptorService::class.java)

        this.limitCache = limitCache;

        id = UUID.randomUUID().toString()
        if (DEBUG) {
            val resId = context.getResources().getIdentifier("debug_id", "string", context.getPackageName())
            if (resId != 0) {
                id = context.getString(resId);
            }
        }
        hereToken = ""
        tomtomToken = ""
    }

    fun verify(purchase: Purchase) {
        val requestBody = RequestBody.create(MediaType.parse("application/json"), purchase.originalJson)
        raptorService.verify(id, requestBody)
                .subscribeOn(Schedulers.io())
                .subscribe({ verificationResponse ->
                    val token = verificationResponse.token
                    if (purchase.sku == BillingConstants.SKU_HERE || DEBUG) {
                        hereToken = token
                    }
                    if (purchase.sku == BillingConstants.SKU_TOMTOM || DEBUG) {
                        tomtomToken = token
                    }
                }, { error ->
                    error.printStackTrace()
                })
    }

    override fun getSpeedLimit(location: Location, lastResponse: LimitResponse?): Observable<LimitResponse>? {
        var defaultResponse = Observable.empty<LimitResponse>()

        val latitude = String.format(Locale.getDefault(), "%.5f", location.latitude)
        val longitude = String.format(Locale.getDefault(), "%.5f", location.longitude)

        if (!hereToken.isEmpty()) {
            val hereResponse = queryRaptorApi(true, latitude, longitude, location)
            defaultResponse = defaultResponse.switchIfEmpty(hereResponse);
        }

        if (!tomtomToken.isEmpty()) {
            val tomtomResponse = queryRaptorApi(false, latitude, longitude, location)
            defaultResponse = defaultResponse.switchIfEmpty(tomtomResponse);
        }

        return defaultResponse
    }

    private fun queryRaptorApi(here: Boolean, latitude: String, longitude: String, location: Location): Observable<LimitResponse>? {
        val raptorQuery = if (here) {
            raptorService.getHere("Bearer " + hereToken, id, latitude, longitude, location.bearing.toInt())
        } else {
            raptorService.getTomtom("Bearer " + tomtomToken, id, latitude, longitude, location.bearing.toInt())
        }

        return raptorQuery
                .flatMapObservable { raptorResponse ->
                    if (raptorResponse.generalSpeedLimit != 0) {
                        val coords = PolyUtil.decode(raptorResponse.polyline).map { latLng ->
                            Coord(latLng.latitude, latLng.longitude)
                        }
                        val response = LimitResponse.builder()
                                .setRoadName(raptorResponse.name)
                                .setSpeedLimit(raptorResponse.generalSpeedLimit!!)
                                .setTimestamp(System.currentTimeMillis())
                                .setCoords(coords)
                                .setOrigin(if (here) LimitResponse.ORIGIN_HERE else LimitResponse.ORIGIN_TOMTOM)
                                .build()

                        limitCache.put(response)

                        Observable.just(response)
                    } else {
                        Observable.empty<LimitResponse>()
                    }
                }
    }


}
