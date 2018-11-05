package com.pluscubed.velociraptor.api

import android.location.Location

interface LimitProvider {

    /**
     * Returns all responses and caches each way received (regardless of whether there is speed limit)
     */
    suspend fun getSpeedLimit(
        location: Location,
        lastResponse: LimitResponse?,
        origin: Int = -1
    ): List<LimitResponse>
}
