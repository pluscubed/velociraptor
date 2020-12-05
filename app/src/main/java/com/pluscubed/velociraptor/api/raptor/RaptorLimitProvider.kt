package com.pluscubed.velociraptor.api.raptor

import android.content.Context
import android.location.Location
import androidx.annotation.WorkerThread
import com.android.billingclient.api.Purchase
import com.google.maps.android.PolyUtil
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.api.*
import com.pluscubed.velociraptor.api.cache.CacheLimitProvider
import com.pluscubed.velociraptor.billing.BillingConstants
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils
import okhttp3.OkHttpClient
import java.util.*

class RaptorLimitProvider(
        private val context: Context,
        client: OkHttpClient,
        private val cacheLimitProvider: CacheLimitProvider
) : LimitProvider {

    private val SERVER_URL = "http://overpass.pluscubed.com:4000/"

    private val raptorService: RaptorService

    private var id: String

    private var hereToken: String
    private var tomtomToken: String

    companion object {
        const val USE_DEBUG_ID = BuildConfig.BUILD_TYPE == "debug"
    }

    init {
        val interceptor = LimitInterceptor(LimitInterceptor.Callback())
        val raptorClient = client.newBuilder()
                .addInterceptor(interceptor)
                .build()
        val raptorRest = LimitFetcher.buildRetrofit(raptorClient, SERVER_URL)
        raptorService = raptorRest.create(RaptorService::class.java)

        id = UUID.randomUUID().toString()
        if (USE_DEBUG_ID) {
            val resId =
                    context.resources.getIdentifier("debug_id", "string", context.getPackageName())
            if (resId != 0) {
                id = context.getString(resId);
            }
        }
        hereToken = ""
        tomtomToken = ""
    }

    /**
     * Returns whether updated token
     */
    @WorkerThread
    fun verify(purchase: Purchase): Boolean {
        var updated = false
        val verificationNetworkResponse = raptorService.verify(id, purchase.originalJson).execute()
        val verificationResponse = Utils.getResponseBody(verificationNetworkResponse)
        val token = verificationResponse.token
        if (purchase.sku == BillingConstants.SKU_HERE || USE_DEBUG_ID) {
            if (hereToken.isEmpty())
                updated = true
            hereToken = token
        }
        if (purchase.sku == BillingConstants.SKU_TOMTOM || USE_DEBUG_ID) {
            if (tomtomToken.isEmpty())
                updated = true
            tomtomToken = token
        }
        return updated
    }

    override fun getSpeedLimit(
            location: Location,
            lastResponse: LimitResponse?,
            origin: Int
    ): List<LimitResponse> {
        val latitude = String.format(Locale.ROOT, "%.5f", location.latitude)
        val longitude = String.format(Locale.ROOT, "%.5f", location.longitude)

        when (origin) {
            LimitResponse.ORIGIN_HERE ->
                if (!hereToken.isEmpty()) {
                    val hereResponse = queryRaptorApi(true, latitude, longitude, location)
                    return listOf(hereResponse)
                }
            LimitResponse.ORIGIN_TOMTOM ->
                if (!tomtomToken.isEmpty()) {
                    val tomtomResponse = queryRaptorApi(false, latitude, longitude, location)
                    return listOf(tomtomResponse)
                }
        }

        return emptyList()
    }

    private fun queryRaptorApi(
            isHere: Boolean,
            latitude: String,
            longitude: String,
            location: Location
    ): LimitResponse {
        val raptorQuery = if (isHere) {
            raptorService.getHere(
                    "Bearer $hereToken",
                    id,
                    latitude,
                    longitude,
                    location.bearing.toInt(),
                    "Metric"
            )
        } else {
            raptorService.getTomtom(
                    "Bearer $tomtomToken",
                    id,
                    latitude,
                    longitude,
                    location.bearing.toInt(),
                    "Metric"
            )
        }

        val debuggingEnabled = PrefUtils.isDebuggingEnabled(context)
        try {
            val raptorNetworkResponse = raptorQuery.execute();
            val raptorResponse = Utils.getResponseBody(raptorNetworkResponse)

            val coords = PolyUtil.decode(raptorResponse.polyline).map { latLng ->
                Coord(latLng.latitude, latLng.longitude)
            }
            val speedLimit = if (raptorResponse.generalSpeedLimit == 0) {
                -1
            } else {
                raptorResponse.generalSpeedLimit!!
            }
            val response = LimitResponse(
                    roadName = if (raptorResponse.name.isEmpty()) "null" else raptorResponse.name,
                    speedLimit = speedLimit,
                    timestamp = System.currentTimeMillis(),
                    coords = coords,
                    origin = getOriginInt(isHere)
            ).initDebugInfo(debuggingEnabled)

            cacheLimitProvider.put(response)

            return response
        } catch (e: Exception) {
            return LimitResponse(
                    error = e,
                    timestamp = System.currentTimeMillis(),
                    origin = getOriginInt(isHere)
            ).initDebugInfo(debuggingEnabled)
        }
    }

    private fun getOriginInt(here: Boolean) =
            if (here) LimitResponse.ORIGIN_HERE else LimitResponse.ORIGIN_TOMTOM


}
