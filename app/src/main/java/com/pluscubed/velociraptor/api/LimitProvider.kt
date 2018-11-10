package com.pluscubed.velociraptor.api

import android.location.Location
import androidx.annotation.WorkerThread

interface LimitProvider {

    /**
     * Returns all responses and caches each way received (regardless of whether there is speed limit)
     */
    @WorkerThread
    fun getSpeedLimit(
        location: Location,
        lastResponse: LimitResponse?,
        origin: Int = -1
    ): List<LimitResponse>
}
